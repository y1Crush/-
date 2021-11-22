package cn.wolfcode.car.business.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Employee {
    /** */
    private Long id;

    /** */
    private String name;

    /** */
    private String email;

    /** */
    private Integer age;
    private Department dept;

    /** */
    private Integer admin;

    /** */
    private Integer status;

    /** */
    private Long deptId;

    /** */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date hiredate;

}