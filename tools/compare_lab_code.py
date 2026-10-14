"""Compare task 1-4 source with the guide using exact token windows.

This is a reproducible local reference metric, not a school's plagiarism score.
It excludes tests, Maven Wrapper, task 5 and the unrelated dept/emp project.
Names and literals are preserved; whitespace and comments are excluded.
"""
import argparse
import json
import re
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET


LEXER = re.compile(
    r'<!--.*?-->|/\*.*?\*/|//[^\n]*|--[^\n]*'
    r'|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    r'|[^\W\d]\w*|\d+(?:\.\d+)?|[^\s]', re.DOTALL
)


def tokens(source):
    source = re.sub(r'(?m)^\s*#(?!\{)[^\n]*$', '', source)
    return [match.group() for match in LEXER.finditer(source)
            if not match.group().startswith(('<!--', '/*', '//', '--'))]


def guide_text(document):
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    with zipfile.ZipFile(document) as archive:
        root = ET.fromstring(archive.read('word/document.xml'))
    paragraphs = [''.join(t.text or '' for t in p.findall('.//w:t', ns))
                  for p in root.findall('.//w:body//w:p', ns)]
    begin = next(i for i, line in enumerate(paragraphs) if line.startswith('任务 1'))
    end = next(i for i in range(begin + 1, len(paragraphs)) if paragraphs[i].startswith('任务 5'))
    return '\n'.join(paragraphs[begin:end])


def coverage(source_tokens, reference_windows, window):
    covered = set()
    for start in range(len(source_tokens) - window + 1):
        if tuple(source_tokens[start:start + window]) in reference_windows:
            covered.update(range(start, start + window))
    return len(covered)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('guide', type=Path)
    parser.add_argument('--project', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--window', type=int, default=12)
    parser.add_argument('--max-percent', type=float, default=30)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    if args.window < 2:
        parser.error('--window must be at least 2')
    reference = tokens(guide_text(args.guide))
    windows = {tuple(reference[i:i + args.window]) for i in range(len(reference) - args.window + 1)}
    root = args.project.resolve()
    files = list((root / 'src/main/java/com/employee').rglob('*.java'))
    files += list((root / 'src/main/resources/com/employee').rglob('*.xml'))
    files += [root / item for item in (
        'pom.xml', 'sql/init.sql', 'src/main/resources/employee-mybatis-config.xml',
        'src/main/resources/jdbc.properties', 'src/main/resources/logback.xml',
    )]
    results = []
    for path in sorted(files):
        source = tokens(path.read_text(encoding='utf-8-sig'))
        matched = coverage(source, windows, args.window)
        results.append({'file': path.relative_to(root).as_posix(), 'tokens': len(source),
                        'matched_tokens': matched, 'percent': round(100 * matched / len(source), 2) if source else 0})
    total = sum(item['tokens'] for item in results)
    matched = sum(item['matched_tokens'] for item in results)
    percent = 100 * matched / total if total else 0
    report = {'metric': 'exact_token_window_coverage', 'window': args.window,
              'scope': 'task 1-4 production sources only; tests and wrapper excluded',
              'limitation': 'Names preserved; no identifier normalization or semantic comparison. Not an institutional plagiarism score.',
              'tokens': total, 'matched_tokens': matched, 'percent': round(percent, 2),
              'threshold': args.max_percent, 'below_threshold': percent < args.max_percent,
              'files': results}
    encoded = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded + '\n', encoding='utf-8')
    print(encoded)
    return 0 if percent < args.max_percent else 1


if __name__ == '__main__':
    raise SystemExit(main())
