package io.github.kang30984.minisso.adapter.outbound.persistence.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 공백으로 구분한 문자열 컬럼 <-> Set<String>.
 * scope와 리다이렉트 URI는 공백을 포함할 수 없으므로(OAuth2 규격) 구분자로 안전하다.
 * 클라이언트당 값이 몇 개뿐이고 항상 함께 읽히므로 별도 테이블 대신 컬럼 하나로 저장한다.
 */
@Converter
class SpaceDelimitedSetConverter : AttributeConverter<Set<String>, String> {
    override fun convertToDatabaseColumn(attribute: Set<String>?): String =
        attribute.orEmpty().sorted().joinToString(" ")

    override fun convertToEntityAttribute(dbData: String?): Set<String> =
        dbData.orEmpty().split(' ').filter { it.isNotBlank() }.toSet()
}
