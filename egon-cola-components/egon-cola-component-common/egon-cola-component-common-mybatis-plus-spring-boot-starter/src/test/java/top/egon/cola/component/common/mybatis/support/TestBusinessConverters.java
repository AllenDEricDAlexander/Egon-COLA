package top.egon.cola.component.common.mybatis.support;

import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Test-only explicit DTO/PO and PO/Model conversion boundaries.
 */
public final class TestBusinessConverters {

    private final BaseConverter<TestBusinessDTO, TestBusinessPO> dtoToPo =
            new BaseConverter<>() {
                @Override
                public TestBusinessPO toTarget(TestBusinessDTO source) {
                    if (source == null) {
                        return null;
                    }
                    return new TestBusinessPO(source.getTitle(), source.getPayload(), "normal");
                }

                @Override
                public TestBusinessDTO toSource(TestBusinessPO target) {
                    if (target == null) {
                        return null;
                    }
                    return new TestBusinessDTO(target.getTitle(), target.getPayload());
                }
            };

    private final BaseConverter<TestBusinessPO, TestBusinessModel> poToModel =
            new BaseConverter<>() {
                @Override
                public TestBusinessModel toTarget(TestBusinessPO source) {
                    if (source == null) {
                        return null;
                    }
                    return new TestBusinessModel().businessValues(source.getTitle(), source.getPayload());
                }

                @Override
                public TestBusinessPO toSource(TestBusinessModel target) {
                    if (target == null) {
                        return null;
                    }
                    return new TestBusinessPO(target.getTitle(), target.getPayload(), "loaded");
                }
            };

    public BaseConverter<TestBusinessDTO, TestBusinessPO> dtoToPo() {
        return dtoToPo;
    }

    public BaseConverter<TestBusinessPO, TestBusinessModel> poToModel() {
        return poToModel;
    }
}
