"""MR 交接信息定向检查；不推断业务验收已经完成。"""
import json
import os
import re
from pathlib import Path

description = os.getenv("CI_MERGE_REQUEST_DESCRIPTION", "")
source = os.getenv("CI_PIPELINE_SOURCE", "local")
result = {"source": source, "sha": os.getenv("CI_COMMIT_SHA", "uncommitted"), "checks": []}
if source == "merge_request_event":
    checks = {
        "Issue 关联": bool(re.search(r"(?:#\d+|/issues/\d+)", description)),
        "验收项": bool(re.search(r"AC-\d+", description)),
        "实际验证说明": "验证" in description,
        "迁移影响说明": "迁移" in description,
    }
    result["checks"] = [{"name": key, "passed": value} for key, value in checks.items()]
    passed = all(checks.values())
else:
    result["checks"] = [{"name": "MR 描述检查", "status": "skipped", "reason": "当前不是 MR 流水线"}]
    passed = True
Path("handoff-report.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(result, ensure_ascii=False))
raise SystemExit(0 if passed else 1)
