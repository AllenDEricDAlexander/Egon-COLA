<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="[=daoFqn]">
    <resultMap id="[=poType]ResultMap" type="[=poFqn]">
        <id column="id" property="id"/>
[#list businessFields as field]
        <result column="[=field.column]" property="[=field.javaName]"/>
[/#list]
        <result column="tenant_id" property="tenantId"/>
        <result column="create_user_id" property="createUserId"/>
        <result column="create_time" property="createTime"/>
        <result column="update_user_id" property="updateUserId"/>
        <result column="update_time" property="updateTime"/>
        <result column="deleted_at" property="deletedAt"/>
        <result column="version" property="version"/>
    </resultMap>
    <sql id="columns">[#list xmlColumns as column][=column][#if column?has_next], [/#if][/#list]</sql>
    <select id="selectActiveById" resultMap="[=poType]ResultMap">
        SELECT <include refid="columns"/> FROM [=tableName] WHERE id = #{id} AND deleted_at IS NULL
    </select>
    <select id="selectActiveByIds" resultMap="[=poType]ResultMap">
        SELECT <include refid="columns"/> FROM [=tableName] WHERE deleted_at IS NULL
        <choose>
            <when test="ids != null and ids.size() &gt; 0">
                AND id IN
                <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </when>
            <otherwise>AND 1 = 0</otherwise>
        </choose>
    </select>
    <update id="deleteVersionedById">
        UPDATE [=tableName]
        SET deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
            update_user_id = #{et.updateUserId},
            update_time = #{et.updateTime},
            version = version + 1
        WHERE id = #{et.id} AND deleted_at IS NULL AND version = #{MP_OPTLOCK_VERSION_ORIGINAL}
    </update>
[#if includeQuery!false]
    <select id="selectByQuery" resultMap="[=poType]ResultMap">
        SELECT <include refid="columns"/> FROM [=tableName]
        WHERE deleted_at IS NULL
[#list filterFields as field]
        <if test="query.[=field.javaName] != null">AND [=field.column] = #{query.[=field.javaName]}</if>
[/#list]
        ORDER BY id ASC
        LIMIT #{limit} OFFSET #{offset}
    </select>
[/#if]
</mapper>
