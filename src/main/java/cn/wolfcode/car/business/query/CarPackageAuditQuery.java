package cn.wolfcode.car.business.query;

import cn.wolfcode.car.common.base.query.QueryObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Calendar;
import java.util.Date;

@Setter
@Getter
public class CarPackageAuditQuery extends QueryObject {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date beginTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endTime;

    /**
     * 搜索时间问题
     * 在程序中设置时间为传入时间当天的23时59分59秒,最好在程序中写优化 不要在数据库中设置
     * @return
     */
    public Date getEndTime() {
        if(this.beginTime != null){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.endTime);
            calendar.set(Calendar.HOUR_OF_DAY,23);
            calendar.set(Calendar.MINUTE,59);
            calendar.set(Calendar.SECOND, 59);
            return calendar.getTime();
        }
        return null;
    }
    private Long auditorId;
    private Integer status;
    private String auditorName;
}
