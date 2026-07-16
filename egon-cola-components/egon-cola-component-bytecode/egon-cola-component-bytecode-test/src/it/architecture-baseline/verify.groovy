assert new File(basedir, ".egon-cola/architecture-baseline.json").text.contains("fingerprints")
assert new File(basedir, "target/egon-cola-architecture/architecture-report.json").text.contains("ARCH-001")
