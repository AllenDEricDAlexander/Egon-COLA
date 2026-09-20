package top.egon.cola.component.common.converter;

import io.github.linpeilie.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BaseConverterContractTest {

    @Test
    void manualImplementationUsesDefaultListConversion() {
        BaseConverter<Category, CategoryDto> converter = new ManualCategoryConverter();

        List<CategoryDto> result = converter.toTargetList(List.of(new Category(1L, "food")));

        assertEquals(List.of(new CategoryDto(1L, "food")), result);
        assertEquals(List.of(new Category(1L, "food")), converter.toSourceList(result));
    }

    @Test
    void legacyDateCodecIsRemovedSoProtocolTimeStaysOwnedByEachMapper() {
        assertFalse(Arrays.stream(BaseConverter.class.getMethods())
                .anyMatch(method -> "map".equals(method.getName())));
        assertFalse(Arrays.stream(BaseConverter.class.getDeclaredMethods())
                .anyMatch(method -> Date.class.equals(method.getReturnType())
                        || Arrays.asList(method.getParameterTypes()).contains(Date.class)));
    }

    @Test
    void forwardProjectionContractStaysOneWay() {
        BaseForwardConverter<Category, CategoryDto> projection = source -> new CategoryDto(source.id(), source.name());

        assertEquals(new CategoryDto(4L, "music"), projection.toTarget(new Category(4L, "music")));
        assertEquals(List.of(), projection.toTargetList(null));
        assertEquals(List.of(new CategoryDto(4L, "music")), projection.toTargetList(List.of(new Category(4L, "music"))));
        assertFalse(Arrays.stream(BaseForwardConverter.class.getMethods())
                .anyMatch(method -> "toSource".equals(method.getName())));
    }

    @Test
    void mapStructImplementationCanImplementBaseConverter() {
        CategoryMapStructConverter converter = Mappers.getMapper(CategoryMapStructConverter.class);

        CategoryDto result = converter.toTarget(new Category(2L, "travel"));

        assertEquals(new CategoryDto(2L, "travel"), result);
        assertEquals(new Category(2L, "travel"), converter.toSource(result));
    }

    @Test
    void mapStructPlusMapperCanBeWrappedByBaseConverter() {
        CategoryMapStructPlusTargetMapper targetMapper = Mappers.getMapper(CategoryMapStructPlusTargetMapper.class);
        CategoryMapStructPlusSourceMapper sourceMapper = Mappers.getMapper(CategoryMapStructPlusSourceMapper.class);
        BaseConverter<Category, CategoryDto> converter = new MapStructPlusCategoryConverter(targetMapper, sourceMapper);

        CategoryDto result = converter.toTarget(new Category(3L, "book"));

        assertEquals(new CategoryDto(3L, "book"), result);
        assertEquals(new Category(3L, "book"), converter.toSource(result));
    }

    record Category(Long id, String name) {
    }

    record CategoryDto(Long id, String categoryName) {
    }

    static final class ManualCategoryConverter implements BaseConverter<Category, CategoryDto> {

        @Override
        public CategoryDto toTarget(Category source) {
            return new CategoryDto(source.id(), source.name());
        }

        @Override
        public Category toSource(CategoryDto target) {
            return new Category(target.id(), target.categoryName());
        }
    }

    @Mapper
    interface CategoryMapStructConverter extends BaseConverter<Category, CategoryDto> {

        @Override
        @Mapping(source = "name", target = "categoryName")
        CategoryDto toTarget(Category source);

        @Override
        @Mapping(source = "categoryName", target = "name")
        Category toSource(CategoryDto target);
    }

    @Mapper
    interface CategoryMapStructPlusTargetMapper extends BaseMapper<Category, CategoryDto> {

        @Override
        @Mapping(source = "name", target = "categoryName")
        CategoryDto convert(Category source);
    }

    @Mapper
    interface CategoryMapStructPlusSourceMapper extends BaseMapper<CategoryDto, Category> {

        @Override
        @Mapping(source = "categoryName", target = "name")
        Category convert(CategoryDto target);
    }

    static final class MapStructPlusCategoryConverter implements BaseConverter<Category, CategoryDto> {

        private final CategoryMapStructPlusTargetMapper targetMapper;

        private final CategoryMapStructPlusSourceMapper sourceMapper;

        MapStructPlusCategoryConverter(CategoryMapStructPlusTargetMapper targetMapper,
                                       CategoryMapStructPlusSourceMapper sourceMapper) {
            this.targetMapper = targetMapper;
            this.sourceMapper = sourceMapper;
        }

        @Override
        public CategoryDto toTarget(Category source) {
            return targetMapper.convert(source);
        }

        @Override
        public Category toSource(CategoryDto target) {
            return sourceMapper.convert(target);
        }
    }
}
