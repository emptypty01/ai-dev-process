"""本机 stdio → Penpot Streamable HTTP 桥；Token 从受限目录读取，不进入项目配置。"""
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

TOKEN_FILE = Path("D:/docker/ai-dev-platform/secrets/penpot-mcp-token")
READ_DESIGN_CODE = "return {file: penpot.currentFile.name, page: penpot.currentPage.name, shapes: penpot.currentPage.root.children.map(s => ({id:s.id,name:s.name,type:s.type}))};"


def adapt_response(data, method):
    if method == "initialize" and "result" in data:
        data["result"]["serverInfo"] = {"name": "local-penpot-design", "version": "1.0.0"}
    if method == "tools/list" and "result" in data:
        data["result"].setdefault("tools", []).append({"name": "read_current_design",
            "description": "只读返回当前连接的 Penpot 文件、页面及顶层元素名称。此工具只执行固定的读取代码。",
            "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
            "annotations": {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True, "openWorldHint": False}})
    return data


def main():
    token = TOKEN_FILE.read_text(encoding="utf-8-sig").strip()
    endpoint = "http://localhost:9001/mcp/stream?" + urllib.parse.urlencode({"userToken": token})
    session = None
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    for line in sys.stdin:
        message = json.loads(line)
        method = message.get("method")
        if method == "tools/call" and message.get("params", {}).get("name") == "read_current_design":
            if message["params"].get("arguments", {}):
                print(json.dumps({"jsonrpc": "2.0", "id": message["id"], "error": {"code": -32602, "message": "No arguments accepted"}}), flush=True)
                continue
            message["params"] = {"name": "execute_code", "arguments": {"code": READ_DESIGN_CODE}}
        headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
        if session:
            headers["Mcp-Session-Id"] = session
            headers["MCP-Protocol-Version"] = "2025-03-26"
        request = urllib.request.Request(endpoint, data=json.dumps(message).encode(), headers=headers)
        try:
            with opener.open(request, timeout=180) as response:
                session = response.headers.get("Mcp-Session-Id", session)
                if response.status == 202:
                    continue
                if "text/event-stream" in response.headers.get("Content-Type", ""):
                    for raw in response:
                        event = raw.decode().strip()
                        if event.startswith("data:"):
                            data = json.loads(event[5:].strip())
                            print(json.dumps(adapt_response(data, method), ensure_ascii=False), flush=True)
                            if data.get("id") == message.get("id"):
                                break
                else:
                    raw = response.read()
                    if raw:
                        print(json.dumps(adapt_response(json.loads(raw), method), ensure_ascii=False), flush=True)
        except Exception as error:
            # 异常可能包含带 Token 的 URL，因此仅报告错误类型与 HTTP 状态。
            if "id" in message:
                result = {"jsonrpc": "2.0", "id": message["id"], "error": {"code": -32000,
                    "message": "Penpot MCP request failed: " + type(error).__name__ + " " + str(getattr(error, "code", ""))}}
                print(json.dumps(result), flush=True)


if __name__ == "__main__":
    main()
