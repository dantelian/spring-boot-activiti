# spring-boot-activiti
待办任务视图（v_task_list）：
CREATE VIEW
    v_task_list ( TASK_ID, PROC_INST_ID, ACT_ID, ACT_NAME, ASSIGNEE, DELEGATION_ID, DESCRIPTION, CREATE_TIME, DUE_DATE, CANDIDATE, PRO_NAME ) AS
SELECT
    `a`.`ID_`           AS `TASK_ID`,
    `a`.`PROC_INST_ID_` AS `PROC_INST_ID`,
    `a`.`TASK_DEF_KEY_` AS `ACT_ID`,
    `a`.`NAME_`         AS `ACT_NAME`,
    `a`.`ASSIGNEE_`     AS `ASSIGNEE`,
    `a`.`DELEGATION_`   AS `DELEGATION_ID`,
    `a`.`DESCRIPTION_`  AS `DESCRIPTION`,
    `a`.`CREATE_TIME_`  AS `CREATE_TIME`,
    `a`.`DUE_DATE_`     AS `DUE_DATE`,
    `i`.`USER_ID`       AS `CANDIDATE`,
    `p`.`NAME_`         AS `PRO_NAME`
FROM
    ((`activiti`.`act_ru_task` `a`
LEFT JOIN
    (
        SELECT DISTINCT
            `u`.`TASK_ID_` AS `TASK_ID_`,
            `u`.`USER_ID`  AS `USER_ID`
        FROM
            (
                SELECT
                    `i`.`TASK_ID_` AS `TASK_ID_`,
                    `i`.`USER_ID_` AS `USER_ID`
                FROM
                    (`activiti`.`act_ru_identitylink` `i`
                JOIN `activiti`.`act_ru_task` `t`)
                WHERE
                    (
                        (
                            `i`.`TASK_ID_` IS NOT NULL
                        )
                    AND
                        (
                            `i`.`USER_ID_` IS NOT NULL
                        )
                    AND
                        (
                            `i`.`TASK_ID_` = `t`.`ID_`
                        )
                    AND isnull(`t`.`ASSIGNEE_`)
                    AND
                        (
                            `i`.`TYPE_` = 'candidate'
                        )
                    )
                UNION
                SELECT
                    `i`.`TASK_ID_` AS `TASK_ID_`,
                    `r`.`USER_ID_` AS `USER_ID`
                FROM
                    ((`activiti`.`act_ru_identitylink` `i`
                JOIN `activiti`.`act_id_membership` `r`)
                JOIN `activiti`.`act_ru_task` `t`)
                WHERE
                    (
                        (
                            `i`.`TASK_ID_` IS NOT NULL
                        )
                    AND
                        (
                            `i`.`GROUP_ID_` IS NOT NULL
                        )
                    AND
                        (
                            `i`.`TASK_ID_` = `t`.`ID_`
                        )
                    AND isnull(`t`.`ASSIGNEE_`)
                    AND
                        (
                            `i`.`TYPE_` = 'candidate'
                        )
                    AND
                        (
                            `i`.`GROUP_ID_` = `r`.`GROUP_ID_`
                        )
                    )
            )
            `u`
    )
    `i`
ON
    (
        (
            `a`.`ID_` = `i`.`TASK_ID_`
        )
    )
    )
LEFT JOIN `activiti`.`act_hi_procinst` `p`
ON
    (
        (
            `a`.`PROC_INST_ID_` = `p`.`PROC_INST_ID_`
        )
    )
    )
WHERE
    (
        `a`.`SUSPENSION_STATE_` = 1
    )
ORDER BY
    `a`.`CREATE_TIME_` DESC

已办任务视图（v_task_history）：
CREATE VIEW
    v_task_history ( TASK_ID, PIID, ACT_NAME, EXECUTOR, END_TIME, PRO_NAME, PROC_DEF_ID, STARTER ) AS
SELECT
    `ht`.`ID_`           AS `TASK_ID`,
    `ht`.`PROC_INST_ID_` AS `PIID`,
    `ht`.`NAME_`         AS `ACT_NAME`,
    `ht`.`ASSIGNEE_`     AS `EXECUTOR`,
    `ht`.`END_TIME_`     AS `END_TIME`,
    `hp`.`NAME_`         AS `PRO_NAME`,
    `hp`.`PROC_DEF_ID_`  AS `PROC_DEF_ID`,
    (
        SELECT
            `t_user`.`username`
        FROM
            `t_user`
        WHERE
            (
                `t_user`.`id` = `hp`.`START_USER_ID_`
            )
    ) AS `STARTER`
FROM
    (`act_hi_taskinst` `ht`
JOIN `act_hi_procinst` `hp`)
WHERE
    (
        (
            `ht`.`PROC_INST_ID_` = `hp`.`PROC_INST_ID_`
        )
    AND
        (
            `ht`.`END_TIME_` IS NOT NULL
        )
    AND
        (
            `ht`.`ASSIGNEE_` IS NOT NULL
        )
    AND
        (
            NOT
            (
                CAST(`ht`.`ID_` AS signed) IN
                (
                    SELECT
                        MIN(CAST(`act_hi_taskinst`.`ID_` AS signed))
                    FROM
                        `act_hi_taskinst`
                    GROUP BY
                        `act_hi_taskinst`.`PROC_INST_ID_`
                )
            )
        )
    )

审核意见记录视图（v_comment_list）：
CREATE VIEW
    v_comment_list ( ID_, MESSAGE_, TASK_ID_, PROC_INST_ID_, NAME_, TIME_, username ) AS
SELECT
    `hc`.`ID_`           AS `ID_`,
    `hc`.`MESSAGE_`      AS `MESSAGE_`,
    `hc`.`TASK_ID_`      AS `TASK_ID_`,
    `hc`.`PROC_INST_ID_` AS `PROC_INST_ID_`,
    `ht`.`NAME_`         AS `NAME_`,
    `hc`.`TIME_`         AS `TIME_`,
    `u`.`username`       AS `username`
FROM
    ((`act_hi_comment` `hc`
LEFT JOIN `act_hi_taskinst` `ht`
ON
    (
        (
            `hc`.`TASK_ID_` = `ht`.`ID_`
        )
    )
    )
LEFT JOIN `t_user` `u`
ON
    (
        (
            `ht`.`ASSIGNEE_` = `u`.`id`
        )
    )
    )
