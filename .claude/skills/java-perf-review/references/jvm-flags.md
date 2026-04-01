# JVM Flags Reference for Java 25 Performance

Read this reference when the review involves JVM configuration, container deployment, or GC tuning.

## Table of Contents
1. GC Selection
2. Compact Object Headers
3. Virtual Thread Diagnostics
4. Startup Optimization
5. Container-Aware Settings
6. Memory Tuning
7. Monitoring and Diagnostics

## 1. GC Selection

| GC | Flag | Best For | Pause Target |
|----|------|----------|-------------|
| Generational ZGC | `-XX:+UseZGC` | Low-latency services, large heaps | Sub-millisecond |
| G1 (default) | `-XX:+UseG1GC` | General purpose, balanced | 200ms default target |
| Shenandoah | `-XX:+UseShenandoahGC` | Low-latency alternative to ZGC | Sub-millisecond |
| Parallel | `-XX:+UseParallelGC` | Batch jobs, throughput priority | Longer pauses OK |

**GC Decision Tree (Java 25):**
- Tiny container (< 2 cores, < 2GB RAM, heap < 100MB) → **Serial GC**
- Batch processing, throughput-only metric → **Parallel GC**
- General web service, balanced latency/throughput → **G1 GC** (default, most RSS-efficient)
- Sub-1ms pauses, large heaps (8GB+), can afford 8-20% CPU overhead → **Generational ZGC**
- Low latency on non-Oracle OpenJDK distributions → **Shenandoah GC**

**Important nuance**: Don't blindly apply ZGC to small containers. ZGC's overhead (concurrent threads, colored pointers) makes it counterproductive for small heaps. G1 is more RSS-efficient in constrained environments. ZGC shines at 8GB+ heaps where pause predictability matters.

Provision **at least 25% memory headroom** for ZGC above the live data set.

```
# Production ZGC configuration (large service)
-XX:+UseZGC
-XX:+UseCompactObjectHeaders
-Xmx4g -Xms4g

# Production G1 configuration (small/medium service)
-XX:+UseG1GC
-XX:+UseCompactObjectHeaders
-Xmx1g -Xms1g
```

Note: In Java 25, ZGC is always generational (non-generational ZGC was removed). The `-XX:+ZGenerational` flag is no longer needed.

## 2. Compact Object Headers

```
-XX:+UseCompactObjectHeaders
```

- Shrinks object headers from 12-16 bytes to 8 bytes
- Saves ~10-20% heap on typical workloads
- Product feature in Java 25 (was experimental in 24)
- Improves cache locality and reduces GC pressure
- No known downsides for standard applications

## 3. Virtual Thread Diagnostics

```
# Detect carrier thread pinning during development/testing
-Djdk.tracePinnedThreads=short

# Full stack trace for pinning (verbose — dev only)
-Djdk.tracePinnedThreads=full
```

**JFR events for production monitoring:**
- `jdk.VirtualThreadPinned` — carrier thread pinning events
- `jdk.VirtualThreadStart` / `jdk.VirtualThreadEnd` — lifecycle tracking

## 4. Startup Optimization

```
# Application Class Data Sharing (AppCDS) — 20-40% startup reduction
# Step 1: Generate class list
java -XX:DumpLoadedClassList=classes.lst -jar app.jar

# Step 2: Create archive
java -Xshare:dump -XX:SharedClassListFile=classes.lst \
     -XX:SharedArchiveFile=app-cds.jsa -jar app.jar

# Step 3: Use archive
java -Xshare:on -XX:SharedArchiveFile=app-cds.jsa -jar app.jar

# Spring Boot 4 built-in CDS support
-Dspring.aot.enabled=true
```

**Spring Boot 4 AOT + CDS combined**: Build-time AOT processing pre-computes bean definitions. Combined with CDS, startup drops from ~8s to ~2-4s for typical services.

## 5. Container-Aware Settings

```
# JVM auto-detects container CPU/memory since Java 10
# Override only if auto-detection is wrong:
-XX:ActiveProcessorCount=4
-XX:MaxRAMPercentage=75.0

# Recommended container memory formula:
# Container limit = Xmx + (off-heap: ~25-30% of Xmx)
# For a 4GB container: -Xmx3g leaves ~1GB for stacks, metaspace, direct buffers
```

**Common mistake**: Setting `-Xmx` equal to container memory limit. This leaves no room for thread stacks (1MB per platform thread, ~30KB per virtual thread), metaspace, native memory, and direct byte buffers.

## 6. Memory Tuning

```
# Set initial and max heap equal to avoid resize pauses
-Xmx4g -Xms4g

# Metaspace (rarely needs tuning)
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m

# Direct byte buffer limit (NIO, Netty)
-XX:MaxDirectMemorySize=512m

# String deduplication (useful for apps with many duplicate strings)
-XX:+UseStringDeduplication  # works with G1 and ZGC
```

## 7. Monitoring and Diagnostics

```
# Enable JFR in production (negligible overhead)
-XX:StartFlightRecording=filename=recording.jfr,maxsize=256m,maxage=12h,settings=profile

# GC logging
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=100m

# Heap dump on OOM
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof

# NativeMemoryTracking (slight overhead — use in staging)
-XX:NativeMemoryTracking=summary
```

## Recommended Production Baseline

```
# Java 25 + Spring Boot 4 production starter
-XX:+UseZGC
-XX:+UseCompactObjectHeaders
-Xmx4g -Xms4g
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
-XX:StartFlightRecording=filename=recording.jfr,maxsize=256m,maxage=12h
-Dspring.aot.enabled=true
```

Adjust `-Xmx` based on actual workload profiling. The above is a starting point, not a universal answer.
