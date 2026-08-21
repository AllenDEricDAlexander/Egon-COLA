/**
 * Compiles reference declarations into immutable policy and one fixed transport
 * mode. These strategies expose cached endpoint snapshots only; they never
 * switch between Gateway and Direct or create transport channels.
 */
package top.egon.cola.component.rpc.consumer.reference;
