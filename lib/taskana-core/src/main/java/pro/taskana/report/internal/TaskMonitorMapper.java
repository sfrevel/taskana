package pro.taskana.report.internal;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import pro.taskana.report.api.Timestamp;
import pro.taskana.report.api.item.DetailedMonitorQueryItem;
import pro.taskana.report.api.item.MonitorQueryItem;
import pro.taskana.report.api.item.TaskQueryItem;
import pro.taskana.report.api.item.TimestampQueryItem;
import pro.taskana.task.api.CustomField;
import pro.taskana.task.api.TaskState;

/** This class is the mybatis mapping of task monitoring. */
@SuppressWarnings({"checkstyle:LineLength", "checkstyle:Indentation"})
public interface TaskMonitorMapper {

  @Select(
      "<script>"
          + "SELECT B.WORKBASKET_KEY, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT T.WORKBASKET_KEY, (DAYS(T.DUE) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT T.WORKBASKET_KEY, DATEDIFF('DAY', CURRENT_TIMESTAMP, T.DUE) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT T.WORKBASKET_KEY, DATE_PART('DAY', T.DUE - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK AS T LEFT JOIN ATTACHMENT AS A ON T.ID = A.TASK_ID "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "T.WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND T.STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND T.CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND T.DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "<if test=\"combinedClassificationFilter != null\">"
          + "AND <foreach collection='combinedClassificationFilter' item='item' separator='OR'> "
          + "T.CLASSIFICATION_ID = #{item.taskClassificationId}"
          + "<if test=\"item.attachmentClassificationId != null\">"
          + "AND A.CLASSIFICATION_ID = #{item.attachmentClassificationId}"
          + "</if>"
          + "</foreach>"
          + "</if>"
          + "AND T.DUE IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.WORKBASKET_KEY, B.AGE_IN_DAYS"
          + "</script>")
  @Results({
    @Result(column = "WORKBASKET_KEY", property = "key"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<MonitorQueryItem> getTaskCountOfWorkbaskets(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter,
      @Param("combinedClassificationFilter")
          List<CombinedClassificationFilter> combinedClassificationFilter);

  @Select(
      "<script>"
          + "SELECT B.WORKBASKET_KEY, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT T.WORKBASKET_KEY, (DAYS(T.PLANNED) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT T.WORKBASKET_KEY, DATEDIFF('DAY', CURRENT_TIMESTAMP, T.PLANNED) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT T.WORKBASKET_KEY, DATE_PART('DAY', T.PLANNED - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK AS T LEFT JOIN ATTACHMENT AS A ON T.ID = A.TASK_ID "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "T.WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND T.STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND T.CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND T.DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "<if test=\"combinedClassificationFilter != null\">"
          + "AND <foreach collection='combinedClassificationFilter' item='item' separator='OR'> "
          + "T.CLASSIFICATION_ID = #{item.taskClassificationId}"
          + "<if test=\"item.attachmentClassificationId != null\">"
          + "AND A.CLASSIFICATION_ID = #{item.attachmentClassificationId}"
          + "</if>"
          + "</foreach>"
          + "</if>"
          + "AND T.PLANNED IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.WORKBASKET_KEY, B.AGE_IN_DAYS"
          + "</script>")
  @Results({
    @Result(column = "WORKBASKET_KEY", property = "key"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<MonitorQueryItem> getTaskCountOfWorkbasketsBasedOnPlannedDate(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter,
      @Param("combinedClassificationFilter")
          List<CombinedClassificationFilter> combinedClassificationFilter);

  @Select(
      "<script>"
          + "SELECT B.CLASSIFICATION_CATEGORY, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT CLASSIFICATION_CATEGORY, (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT CLASSIFICATION_CATEGORY, DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT CLASSIFICATION_CATEGORY, DATE_PART('DAY', DUE - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "AND DUE IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.CLASSIFICATION_CATEGORY, B.AGE_IN_DAYS "
          + "</script>")
  @Results({
    @Result(column = "CLASSIFICATION_CATEGORY", property = "key"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<MonitorQueryItem> getTaskCountOfCategories(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter);

  @Select(
      "<script>"
          + "SELECT B.CLASSIFICATION_KEY, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT CLASSIFICATION_KEY, (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT CLASSIFICATION_KEY, DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT CLASSIFICATION_KEY, DATE_PART('DAY', DUE - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "AND DUE IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.CLASSIFICATION_KEY, B.AGE_IN_DAYS "
          + "</script>")
  @Results({
    @Result(column = "CLASSIFICATION_KEY", property = "key"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<MonitorQueryItem> getTaskCountOfClassifications(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter);

  @Select(
      "<script>"
          + "SELECT B.TASK_CLASSIFICATION_KEY, B.ATTACHMENT_CLASSIFICATION_KEY, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT T.CLASSIFICATION_KEY as TASK_CLASSIFICATION_KEY, A.CLASSIFICATION_KEY as ATTACHMENT_CLASSIFICATION_KEY, (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT T.CLASSIFICATION_KEY as TASK_CLASSIFICATION_KEY, A.CLASSIFICATION_KEY as ATTACHMENT_CLASSIFICATION_KEY, DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT T.CLASSIFICATION_KEY as TASK_CLASSIFICATION_KEY, A.CLASSIFICATION_KEY as ATTACHMENT_CLASSIFICATION_KEY, DATE_PART('DAY', DUE - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK AS T LEFT JOIN ATTACHMENT AS A ON T.ID = A.TASK_ID "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "T.WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "AND DUE IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.TASK_CLASSIFICATION_KEY, B.ATTACHMENT_CLASSIFICATION_KEY, B.AGE_IN_DAYS "
          + "</script>")
  @Results({
    @Result(column = "TASK_CLASSIFICATION_KEY", property = "key"),
    @Result(column = "ATTACHMENT_CLASSIFICATION_KEY", property = "attachmentKey"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<DetailedMonitorQueryItem> getTaskCountOfDetailedClassifications(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter);

  @Select(
      "<script>"
          + "SELECT B.CUSTOM_FIELD, B.AGE_IN_DAYS, COUNT(B.AGE_IN_DAYS) AS NUMBER_OF_TASKS FROM ("
          + "<if test=\"_databaseId == 'db2'\">SELECT ${customField} as CUSTOM_FIELD, (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'h2'\">SELECT ${customField} as CUSTOM_FIELD, DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) as AGE_IN_DAYS </if> "
          + "<if test=\"_databaseId == 'postgres'\">SELECT ${customField} as CUSTOM_FIELD, DATE_PART('DAY', DUE - CURRENT_TIMESTAMP) as AGE_IN_DAYS </if> "
          + "FROM TASK "
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "AND DUE IS NOT NULL "
          + "</where>"
          + ") AS B "
          + "GROUP BY B.CUSTOM_FIELD, B.AGE_IN_DAYS "
          + "</script>")
  @Results({
    @Result(column = "CUSTOM_FIELD", property = "key"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "NUMBER_OF_TASKS", property = "numberOfTasks")
  })
  List<MonitorQueryItem> getTaskCountOfCustomFieldValues(
      @Param("customField") CustomField customField,
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter);

  @Select(
      "<script>"
          + "SELECT T.ID FROM TASK T "
          + "<if test=\"joinWithAttachments\">"
          + "LEFT JOIN ATTACHMENT A ON T.ID = A.TASK_ID "
          + "</if>"
          + "<where>"
          + "<if test=\"workbasketIds != null\">"
          + "T.WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test=\"states != null\">"
          + "AND T.STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test=\"categories != null\">"
          + "AND T.CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test=\"domains != null\">"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND T.CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND T.CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "AND T.DUE IS NOT NULL AND ( "
          + "<foreach collection='selectedItems' item='selectedItem' separator=' OR '>"
          + "#{selectedItem.key} = T.${groupedBy} AND "
          + "<if test=\"joinWithAttachments\">"
          + "A.CLASSIFICATION_KEY = #{selectedItem.subKey} AND "
          + "</if>"
          + "<if test=\"_databaseId == 'db2'\">"
          + "#{selectedItem.upperAgeLimit} >= (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) AND "
          + "#{selectedItem.lowerAgeLimit} &lt;= (DAYS(DUE) - DAYS(CURRENT_TIMESTAMP)) "
          + "</if> "
          + "<if test=\"_databaseId == 'h2'\">"
          + "#{selectedItem.upperAgeLimit} >= DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) AND "
          + "#{selectedItem.lowerAgeLimit} &lt;= DATEDIFF('DAY', CURRENT_TIMESTAMP, DUE) "
          + "</if> "
          + "<if test=\"_databaseId == 'postgres'\">"
          + "#{selectedItem.upperAgeLimit} >= DATE_PART('day', DUE - CURRENT_TIMESTAMP ) AND "
          + "#{selectedItem.lowerAgeLimit} &lt;= DATE_PART('day', DUE - CURRENT_TIMESTAMP ) "
          + "</if> "
          + "</foreach>) "
          + "</where>"
          + "<if test=\"_databaseId == 'db2'\">with UR </if> "
          + "</script>")
  List<String> getTaskIdsForSelectedItems(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter,
      @Param("groupedBy") String groupedBy,
      @Param("selectedItems") List<SelectedItem> selectedItems,
      @Param("joinWithAttachments") boolean joinWithAttachments);

  @Select(
      "<script>"
          + "SELECT DOMAIN, STATE, COUNT(STATE) as COUNT "
          + "FROM TASK "
          + "<where>"
          + "<if test='domains != null'>"
          + "DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='states != null'>"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "</where>"
          + "GROUP BY DOMAIN, STATE"
          + "</script>")
  @Results({
    @Result(column = "DOMAIN", property = "domain"),
    @Result(column = "STATE", property = "state"),
    @Result(column = "COUNT", property = "count"),
  })
  List<TaskQueryItem> getTasksCountByState(
      @Param("domains") List<String> domains, @Param("states") List<TaskState> states);

  @Select(
      "<script>"
          + "SELECT DISTINCT ${customField} "
          + "FROM TASK "
          + "<where>"
          + "<if test='workbasketIds != null'>"
          + "WORKBASKET_ID IN (<foreach collection='workbasketIds' item='workbasketId' separator=','>#{workbasketId}</foreach>) "
          + "</if>"
          + "<if test='states != null'>"
          + "AND STATE IN (<foreach collection='states' item='state' separator=','>#{state}</foreach>) "
          + "</if>"
          + "<if test='categories != null'>"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test='domains != null'>"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "</where>"
          + "</script>")
  List<String> getCustomAttributeValuesForReport(
      @Param("workbasketIds") List<String> workbasketIds,
      @Param("states") List<TaskState> states,
      @Param("categories") List<String> categories,
      @Param("domains") List<String> domains,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter,
      @Param("customField") CustomField customField);

  @Select(
      "<script>"
          + "SELECT A.AGE_IN_DAYS, A.ORG_LEVEL_1, A.ORG_LEVEL_2, A.ORG_LEVEL_3, A.ORG_LEVEL_4, "
          + "'${status}' AS STATUS, COUNT(A.AGE_IN_DAYS) AS COUNT FROM ("
          // This subquery prevents the repetition of the AGE_IN_DAYS column calculation
          // (like everywhere else in the Mappers...)in the group by clause.
          // DB2 is not able to reuse computed columns in the group by statement. Even if this adds
          // a little
          // overhead / complexity. It's worth the trade-off of not computing the AGE_IN_DAYS column
          // twice.
          + "SELECT W.ORG_LEVEL_1, W.ORG_LEVEL_2, W.ORG_LEVEL_3, W.ORG_LEVEL_4, "
          + "<if test=\"_databaseId == 'db2'\">(DAYS(T.${status}) - DAYS(CURRENT_TIMESTAMP))</if>"
          + "<if test=\"_databaseId == 'h2'\">DATEDIFF('DAY', CURRENT_TIMESTAMP, T.${status})</if>"
          + "<if test=\"_databaseId == 'postgres'\">DATE_PART('DAY', T.${status} - CURRENT_TIMESTAMP)</if>"
          + " as AGE_IN_DAYS "
          + "FROM TASK AS T INNER JOIN WORKBASKET AS W ON T.WORKBASKET_KEY=W.KEY "
          + "<where>"
          + "<if test=\"status.name() == 'COMPLETED'\">"
          + "T.COMPLETED IS NOT NULL"
          + "</if>"
          + "<if test='categories != null'>"
          + "AND CLASSIFICATION_CATEGORY IN (<foreach collection='categories' item='category' separator=','>#{category}</foreach>) "
          + "</if>"
          + "<if test='classificationIds != null'>"
          + "AND CLASSIFICATION_ID IN (<foreach collection='classificationIds' item='classificationId' separator=','>#{classificationId}</foreach>) "
          + "</if>"
          + "<if test='excludedClassificationIds != null'>"
          + "AND CLASSIFICATION_ID NOT IN (<foreach collection='excludedClassificationIds' item='excludedClassificationId' separator=','>#{excludedClassificationId}</foreach>) "
          + "</if>"
          + "<if test='domains != null'>"
          + "AND DOMAIN IN (<foreach collection='domains' item='domain' separator=','>#{domain}</foreach>) "
          + "</if>"
          + "<if test='customAttributeFilter != null'>"
          + "AND (<foreach collection='customAttributeFilter.keys' item='key' separator=' AND '>(${key} = '${customAttributeFilter.get(key)}')</foreach>) "
          + "</if>"
          + "</where>"
          + ") AS A "
          + "GROUP BY A.AGE_IN_DAYS, A.ORG_LEVEL_1, A.ORG_LEVEL_2, A.ORG_LEVEL_3, A.ORG_LEVEL_4 "
          + "</script>")
  @Results({
    @Result(column = "STATUS", property = "status"),
    @Result(column = "AGE_IN_DAYS", property = "ageInDays"),
    @Result(column = "COUNT", property = "count"),
    @Result(column = "ORG_LEVEL_1", property = "orgLevel1"),
    @Result(column = "ORG_LEVEL_2", property = "orgLevel2"),
    @Result(column = "ORG_LEVEL_3", property = "orgLevel3"),
    @Result(column = "ORG_LEVEL_4", property = "orgLevel4")
  })
  List<TimestampQueryItem> getTasksCountForStatusGroupedByOrgLevel(
      @Param("status") Timestamp status,
      @Param("categories") List<String> categories,
      @Param("classificationIds") List<String> classificationIds,
      @Param("excludedClassificationIds") List<String> excludedClassificationIds,
      @Param("domains") List<String> domains,
      @Param("customAttributeFilter") Map<CustomField, String> customAttributeFilter);
}