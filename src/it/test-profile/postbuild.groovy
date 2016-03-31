def buildLog = new File(basedir, "build.log");
assert buildLog.exists()
activeProfiles = "Active Maven profiles:\n"+
        "it-repo (source: external)\n"+
        "profile-5 (source: org.nuxeo.build:test-profile-module:"+projectVersion+")\n"+
        "profile-6 (source: org.nuxeo.build:test-profile-module:"+projectVersion+")\n"+
        "profile-1 (source: org.nuxeo.build:test-profile-parent:"+projectVersion+")\n"+
        "profile-4 (source: org.nuxeo.build:test-profile-parent:"+projectVersion+")"
assert buildLog.text.contains(activeProfiles)
