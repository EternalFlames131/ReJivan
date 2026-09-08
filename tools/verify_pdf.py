# verify_pdf.py
# Prints page count and checks key content of the SanjivanAI concept PDF.
# Usage: python -X utf8 tools\verify_pdf.py path\file.pdf
import sys
from pypdf import PdfReader

path = sys.argv[1] if len(sys.argv) > 1 else "docs/SanjivanAI_Concept_Document_v1.1.pdf"
r = PdfReader(path)
text = "".join(p.extract_text() for p in r.pages)
low = text.lower()

print("pages:", len(r.pages))
checks = ["SANJIVANAI", "Virtual Ward", "camera", "15 October 2026", "privacy"]
failed = False
for kw in checks:
    ok = kw.lower() in low
    print(("OK   " if ok else "MISS "), kw)
    if not ok:
        failed = True

sys.exit(1 if failed else 0)