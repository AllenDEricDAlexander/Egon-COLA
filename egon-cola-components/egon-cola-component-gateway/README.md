# Egon COLA Gateway Component

The Gateway component is a self-built HTTP and RPC gateway platform. It is
implemented as a layered component with independently deployable Engine and
Admin applications and independently consumable Starter and HTTP Provider
Runtime artifacts.

## Modules

- `egon-cola-component-gateway-contract`: stable cross-process contracts.
- `egon-cola-component-gateway-core`: framework-free data-plane model and SPI.
- `egon-cola-component-gateway-engine`: executable gateway data plane.
- `egon-cola-component-gateway-admin`: executable management control plane.
- `egon-cola-component-gateway-starter`: provider interface definition reporting.
- `egon-cola-component-gateway-provider-runtime`: HTTP provider DDC lease runtime.
- `egon-cola-component-gateway-admin-web`: independently built React admin UI.
- `egon-cola-component-gateway-test`: real-provider and end-to-end test projects.

Only the Starter and Provider Runtime artifacts are exported by the public
components BOM. The Engine, Admin, Contract, Core, and test artifacts are
internal platform modules.

The implementation is delivered incrementally according to the approved
Gateway specifications under `docs/superpowers/specs`.
