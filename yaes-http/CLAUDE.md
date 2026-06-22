## yaes-http

HTTP module for λÆS.

### SBT Project Naming

The HTTP server subproject is defined in `build.sbt` as `lazy val server = project.in(file("yaes-http/server"))`. Use the **project name**, not the directory path:

```bash
# ✅ CORRECT
sbt "server/compile"
sbt "server/test"
sbt "server/testOnly io.yaes.http.server.parsing.HttpParserSpec"

# ❌ INCORRECT
sbt "yaes-http/server/compile"
```
