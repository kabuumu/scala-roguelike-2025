---
description: Run sbt tests to verify code changes
---

// turbo-all

1. Run the full test suite:
```bash
cd /Users/robhawkes/Documents/personal/scala-roguelike-2025 && sbt test 2>&1 | tail -25
```

2. Check the output for the summary line. All tests should pass:
```
[info] All tests passed.
```

3. If any tests fail, the output will show:
```
[error] Failed tests:
[error]         <test suite names>
```

Fix the failing tests before proceeding with further changes.
