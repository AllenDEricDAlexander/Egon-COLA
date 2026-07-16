def report = new File(basedir, "application/target/egon-cola-architecture/architecture-report.json")
assert report.isFile()
assert report.text.contains('"total" : 0')
