package com.ccloud.module.infra.service.db;

import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.IndexInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DbDocServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DbDocServiceImpl dbDocService;

    @Mock
    private DatabaseTableService databaseTableService;

    @BeforeEach
    void stubTables() {
        ColumnInfo name = new ColumnInfo("name", "varchar(64)", "varchar", "名称|说明", true, "a<b", false, false, 1, 64L);
        TableInfo table = new TableInfo("t_demo", "<script>alert(1)</script>|表", List.of(name),
                List.of(new IndexInfo("uk_name", true, List.of("name"))));
        when(databaseTableService.getTableListWithDetails(0L)).thenReturn(List.of(table));
        when(databaseTableService.getDatabaseName(0L)).thenReturn("ccloud");
    }

    @Test
    void exportHtml_内容齐全且全部转义() {
        String html = dbDocService.exportHtml(0L);

        assertThat(html).contains("<title>ccloud 数据库文档</title>", "t_demo", "varchar(64)", "uk_name");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("a&lt;b");
    }

    @Test
    void exportMarkdown_表格单元格中的竖线与换行被转义() {
        String markdown = dbDocService.exportMarkdown(0L);

        assertThat(markdown).contains("| name | varchar(64) |");
        assertThat(markdown).contains("名称\\|说明");
        assertThat(markdown).contains("&lt;script&gt;");
        assertThat(markdown).doesNotContain("<script>");
    }
}
