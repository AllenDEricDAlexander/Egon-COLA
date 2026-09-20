package top.egon.cola.component.common.core.pojo;

import java.io.Serializable;

/**
 * Normative marker for every governed data carrier.
 *
 * <p>The contract deliberately declares no field and no method so implementing it never changes
 * the serialized shape of a record or bean.</p>
 */
public interface BasePojo extends Serializable {
}
