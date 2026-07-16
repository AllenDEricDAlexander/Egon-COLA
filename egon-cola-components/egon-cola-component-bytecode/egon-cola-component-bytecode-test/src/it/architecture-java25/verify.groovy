def classFile = new File(basedir, "target/classes/sample/domain/Java25Record.class")
assert classFile.isFile()

classFile.withDataInputStream { input ->
    assert input.readInt() == 0xCAFEBABE as int
    input.readUnsignedShort()
    assert input.readUnsignedShort() == 69
}

def report = new File(basedir, "target/egon-cola-architecture/architecture-report.json")
assert report.isFile()
assert report.text.contains('"total" : 0')
