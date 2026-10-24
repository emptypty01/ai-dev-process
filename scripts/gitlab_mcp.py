"""仅暴露本地示例项目 Issue 的只读 MCP 工具。"""
import json
import sys
import urllib.request
from pathlib import Path

PLATFORM = Path("D:/docker/ai-dev-platform")


def main():
    for line in sys.stdin:
        request = json.loads(line)
        if "id" not in request:
            continue
        method = request.get("method")
        result = {}
        try:
            if method == "initialize":
                result = {"protocolVersion": request.get("params", {}).get("protocolVersion", "2025-03-26"),
                          "capabilities": {"tools": {}}, "serverInfo": {"name": "local-gitlab-issues", "version": "1.0.0"}}
            elif method == "tools/list":
                result = {"tools": [{"name": "read_issue", "description": "读取本地示例项目的 Issue 正文、标签与评论；只读。",
                    "inputSchema": {"type": "object", "properties": {"iid": {"type": "integer", "minimum": 1}}, "required": ["iid"], "additionalProperties": False},
                    "annotations": {"readOnlyHint": True, "destructiveHint": False}}]}
            elif method == "tools/call":
                params = request.get("params", {})
                if params.get("name") != "read_issue":
                    raise ValueError("Unknown tool")
                iid = params.get("arguments", {}).get("iid")
                if isinstance(iid, bool) or not isinstance(iid, int) or iid < 1:
                    raise ValueError("iid must be a positive integer")
                project = json.loads((PLATFORM / "project.json").read_text(encoding="utf-8-sig"))
                token = (PLATFORM / "secrets/gitlab-api-token").read_text(encoding="utf-8-sig").strip()
                url = f"http://localhost:8088/api/v4/projects/{project['id']}/issues/{iid}"
                def get(path):
                    query = urllib.request.Request(path, headers={"PRIVATE-TOKEN": token})
                    with urllib.request.urlopen(query, timeout=30) as response:
                        return json.load(response)
                result = {"content": [{"type": "text", "text": json.dumps({"issue": get(url),
                    "notes": get(url + "/notes?sort=asc&per_page=100")}, ensure_ascii=False)}]}
            elif method != "ping":
                print(json.dumps({"jsonrpc": "2.0", "id": request["id"], "error": {"code": -32601, "message": "Unknown method"}}), flush=True)
                continue
            print(json.dumps({"jsonrpc": "2.0", "id": request["id"], "result": result}, ensure_ascii=False), flush=True)
        except Exception as error:
            print(json.dumps({"jsonrpc": "2.0", "id": request["id"], "error": {"code": -32000, "message": type(error).__name__}}), flush=True)


if __name__ == "__main__":
    main()
