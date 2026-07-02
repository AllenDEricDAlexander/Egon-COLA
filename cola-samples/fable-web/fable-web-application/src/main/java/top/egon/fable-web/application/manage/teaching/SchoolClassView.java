package top.egon.fable-web.application.manage.teaching;

import java.util.List;

public record SchoolClassView(String id, String name, String gradeName, List<String> userIds) {
}
