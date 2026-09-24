Sable's sub-levels each contain their own lighting sections, and lighting data.

We need Flywheel shaders to be aware of this, so we change and override the lighting storage, LUT, and shaders to respect an additional "scene ID".

I'm not happy with the large amounts of duplicated shader code in these overrides, but it's the route we are going with for now.

On 1.20.1 these are rebased onto the shaders of Flywheel 1.0.5, the version Create 6.0.8 bundles (no `flw_vertexId` variable, `_flw_vertexOffset` uniform, no `ambientOcclusion` material flag).

Reference https://github.com/Engine-Room/Flywheel/tree/1.21.1/dev for the original shaders and lighting code that these overrides are based on.