#!/usr/bin/env python3
import json, os, sys, urllib.parse, urllib.request
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).parent
CONFIG = ROOT / "config.json"
HISTORY = ROOT / "history.json"


def load_json(path, default):
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def save_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def http_json(url, method="GET", payload=None, headers=None):
    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
    req = urllib.request.Request(url, data=data, method=method, headers={"Content-Type": "application/json", **(headers or {})})
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def telegram_send(token, chat_id, text):
    return http_json(f"https://api.telegram.org/bot{token}/sendMessage", "POST", {"chat_id": chat_id, "text": text})


def generate(cfg, prompt):
    provider = cfg.get("provider", "groq").lower()
    model = cfg.get("model", "openai/gpt-oss-120b")
    if provider == "gemini":
        key = os.environ["GEMINI_API_KEY"]
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{urllib.parse.quote(model)}:generateContent?key={urllib.parse.quote(key)}"
        out = http_json(url, "POST", {"contents": [{"parts": [{"text": prompt}]}]})
        return out["candidates"][0]["content"]["parts"][0]["text"].strip()
    key = os.environ["OPENAI_API_KEY"] if provider in ("openai", "gpt") else os.environ["GROQ_API_KEY"]
    base = "https://api.openai.com/v1/chat/completions" if provider in ("openai", "gpt") else "https://api.groq.com/openai/v1/chat/completions"
    out = http_json(base, "POST", {"model": model, "messages": [{"role": "user", "content": prompt}], "max_tokens": int(cfg.get("max_length", 700))}, {"Authorization": f"Bearer {key}"})
    return out["choices"][0]["message"]["content"].strip()


def repeat_allowed(instruction):
    text = instruction.lower()
    return any(word in text for word in ("разреши повтор", "повторы разреш", "можно повтор", "allow repeat", "repeat allowed"))


def should_run(cfg, now, force=False):
    if force:
        return True
    if not cfg.get("enabled", False):
        return False
    day = now.strftime("%a").lower()[:3]
    current = now.strftime("%H:%M")
    return any(item.get("time") == current and day in item.get("days", []) for item in cfg.get("schedules", []))


def main(force=False):
    cfg = load_json(CONFIG, {})
    tz = ZoneInfo(cfg.get("timezone", "UTC"))
    now = datetime.now(tz)
    if not should_run(cfg, now, force):
        print("No scheduled run", now.isoformat())
        return 0
    token = os.environ.get("TELEGRAM_BOT_TOKEN", "")
    targets = [str(x) for x in cfg.get("chat_ids", [])]
    if not token or not targets:
        raise RuntimeError("TELEGRAM_BOT_TOKEN or chat_ids is missing")
    history = load_json(HISTORY, [])
    recent = [item.get("text", "") for item in history[-int(cfg.get("history_limit", 20)):]]
    instruction = cfg.get("instruction", "")
    prompt = f"Тема: {cfg.get('topic', '')}\nЯзык: {cfg.get('language', 'русский')}\nИнструкция: {instruction}\n"
    if not repeat_allowed(instruction):
        prompt += "Создай новый текст и не повторяй эти последние тексты:\n" + "\n---\n".join(recent)
    text = generate(cfg, prompt)
    if not repeat_allowed(instruction) and text in recent:
        text = generate(cfg, prompt + "\nСделай другую формулировку.")
    result = {"time": now.isoformat(), "text": text, "targets": [], "provider": cfg.get("provider"), "model": cfg.get("model")}
    for chat_id in targets:
        try:
            response = telegram_send(token, chat_id, text)
            result["targets"].append({"chat_id": chat_id, "ok": bool(response.get("ok")), "message_id": response.get("result", {}).get("message_id")})
        except Exception as exc:
            result["targets"].append({"chat_id": chat_id, "ok": False, "error": str(exc)})
    history.append(result)
    save_json(HISTORY, history[-int(cfg.get("history_limit", 20)):])
    failures = [x for x in result["targets"] if not x.get("ok")]
    print(json.dumps({"status": "partial" if failures else "success", "result": result}, ensure_ascii=False))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main("--now" in sys.argv))
