/**
 * Registry-neutral RPC endpoint selection strategies.
 *
 * <p>This package consumes immutable endpoint snapshots and returns a selected
 * endpoint. It neither discovers services nor creates or closes transport
 * channels, and it never performs cross-mode fallback.
 */
package top.egon.cola.component.rpc.consumer.loadbalance;
