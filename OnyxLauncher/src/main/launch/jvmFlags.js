/**
 * Section 0.2 — JVM flags applied when launching OnyxLoader.
 */
function buildJvmFlags(ramGb) {
  const xms = Math.min(2, ramGb);
  const xmx = Math.max(2, ramGb);

  return [
    '-server',
    `-Xms${xms}G`,
    `-Xmx${xmx}G`,
    '-XX:+UseG1GC',
    '-XX:MaxGCPauseMillis=1',
    '-XX:G1HeapRegionSize=32m',
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:+ParallelRefProcEnabled',
    '-XX:+AlwaysPreTouch',
    '-XX:+DisableExplicitGC',
    '-XX:+UseStringDeduplication',
    '-XX:+OptimizeStringConcat',
    '-XX:+UseFastUnorderedTimeStamps',
    '-Dfml.ignoreInvalidMinecraftCertificates=true',
    '-Dfml.ignorePatchDiscrepancies=true',
    '-Djava.awt.headless=false',
    '-XX:+AggressiveOpts'
  ];
}

module.exports = { buildJvmFlags };
