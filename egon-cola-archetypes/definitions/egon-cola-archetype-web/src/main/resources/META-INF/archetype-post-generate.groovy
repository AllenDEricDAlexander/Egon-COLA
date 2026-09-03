def generatedProjectDir = new File(request.outputDirectory, request.artifactId)
def mvnw = new File(generatedProjectDir, "mvnw")

if (mvnw.isFile()) {
    mvnw.setExecutable(true, false)
}
