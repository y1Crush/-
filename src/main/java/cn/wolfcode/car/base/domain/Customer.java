package cn.wolfcode.car.base.domain;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Customer {
    private Long id; //主键
    private String name;    //客户名称
    private String phone;   //客户手机
    private Integer age;    //客户年龄
}