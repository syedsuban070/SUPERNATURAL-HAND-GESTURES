package com.hyperpixelacity.app.core.ar
/** Explicit baseline: no inferred finger mask and no competing AR camera session. */
interface OcclusionProvider { val description: String; val available: Boolean }
class NoOcclusion : OcclusionProvider { override val description="Standard compositing • depth unavailable";override val available=false }
