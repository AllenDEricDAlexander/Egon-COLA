package top.egon.cola.archetype.source.service.adapter.pojo.dto;

/** Normalized business rejection carried by every native facade response envelope. */
public record FacadeFailureDTO(String code, String message) {
}
