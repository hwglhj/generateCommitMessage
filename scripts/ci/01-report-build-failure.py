"""Expose Gradle and JUnit failures in GitHub check annotations."""

from pathlib import Path
import xml.etree.ElementTree as ET


def annotate(title, detail):
    escaped = detail[-16000:].replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error title={title}::{escaped}")


for report in sorted(Path("build/test-results/test").glob("TEST-*.xml")):
    suite = ET.parse(report).getroot()
    for case in suite.iter("testcase"):
        for failure in (*case.findall("failure"), *case.findall("error")):
            annotate("Test failed", f"{case.get('classname')}.{case.get('name')}\n{failure.text or failure.get('message', '')}")

log = Path("build.log")
if log.exists():
    text = log.read_text(encoding="utf-8", errors="replace")
    marker = "FAILURE: Build failed with an exception."
    annotate("Gradle build failed", text[text.rfind(marker):] if marker in text else text)
