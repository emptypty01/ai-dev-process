"""本地 GitLab Issue 只读入口；凭据仅从平台受限目录读取。"""
import argparse
import json
import urllib.request
from pathlib import Path

PLATFORM = Path("D:/docker/ai-dev-platform")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["read"])
    parser.add_argument("iid", type=int)
    args = parser.parse_args()
    project = json.loads((PLATFORM / "project.json").read_text(encoding="utf-8-sig"))
    token = (PLATFORM / "secrets/gitlab-api-token").read_text(encoding="utf-8-sig").strip()
    path = f"http://localhost:8088/api/v4/projects/{project['id']}/issues/{args.iid}"
    def get(url):
        request = urllib.request.Request(url, headers={"PRIVATE-TOKEN": token})
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    issue = get(path)
    notes = get(path + "/notes?sort=asc&per_page=100")
    print(json.dumps({"issue": issue, "notes": notes}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
