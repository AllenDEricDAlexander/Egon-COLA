def text = new File(basedir, "target/egon-cola-architecture/architecture-report.json").text
assert text.contains("ARCH-001")
