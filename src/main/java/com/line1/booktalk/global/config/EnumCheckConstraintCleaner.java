package com.line1.booktalk.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL에서 enum 컬럼에 자동으로 생긴 CHECK 규칙을 서버 시작 때 지운다.
 *
 * Hibernate는 테이블을 처음 만들 때 "type은 MIMETIC, ONOMATOPOEIA, IDIOM 중 하나" 같은 CHECK 규칙을 만든다.
 * 그런데 ddl-auto=update는 나중에 enum 값이 늘어나도 이 규칙을 고쳐 주지 않아서,
 * 새 값(예: INTERJECTION)을 저장하면 서버가 실패한다. 값 검사는 자바 enum이 이미 하므로 DB 규칙은 지워도 된다.
 * 시드 데이터보다 먼저 실행되도록 가장 앞 순서로 둔다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EnumCheckConstraintCleaner implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                return; // 로컬 H2는 매번 새로 만들어서 필요 없다
            }
            List<String[]> checks = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("""
                         select rel.relname, con.conname
                         from pg_constraint con
                         join pg_class rel on rel.oid = con.conrelid
                         join pg_namespace ns on ns.oid = rel.relnamespace
                         where con.contype = 'c' and ns.nspname = current_schema()
                         """)) {
                while (rs.next()) {
                    checks.add(new String[]{rs.getString(1), rs.getString(2)});
                }
            }
            for (String[] check : checks) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("alter table \"" + check[0] + "\" drop constraint if exists \"" + check[1] + "\"");
                }
                log.info("오래된 CHECK 규칙 삭제 table={} constraint={}", check[0], check[1]);
            }
        }
    }
}
