def buildLog = new File(basedir, "build.log");
assert buildLog.exists()
activeProfiles = "Active Maven profiles:\n"+
"it-repo (source: external)\n"+
"profile-5 (source: org.nuxeo.build:test-profile-module:2.0.5-SNAPSHOT)\n"+
"profile-6 (source: org.nuxeo.build:test-profile-module:2.0.5-SNAPSHOT)\n"+
"profile-1 (source: org.nuxeo.build:test-profile-parent:2.0.5-SNAPSHOT)\n"+
"profile-4 (source: org.nuxeo.build:test-profile-parent:2.0.5-SNAPSHOT)"
assert buildLog.text.contains(activeProfiles)
