**广告系统应该需要实现的最基本的功能**

广告投放系统 > ->  既然是广告系统 ， 一定得有广告数据 ， 数据当然是由广告主或代理商投放 ， 那么 ， 也就需要有个投放广告的平台，这就是广告投放系统

广告检索系统 > ->  媒体方对广告系统发起请求，广告系统能够检索符合要求的广告数据，这就是广告检索系统的核心功能

**完整的广告系统又需要包含哪些子系统呢  ？**

曝光监测系统 > ->  监测广告数据的曝光记录

报表系统 > ->  构建广告数据报表，比如广告 A  在地域 B 中一共曝光了多少次，主要是 OLAP  的过程

扣费系统 > ->  广告的每一次曝光都是需要扣费的，且这个系统里面负责了将广告数据置位的功能

pom文件中添加：

```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

在微服务架构中，后端服务往往不直接开放给调用端，而是通过一个服务网关的 根据请求的  url ，路由到相应的服务，即实现请求转发

Zuul  提供了服务网关的功能 ， 可以实现负载均衡 、 反向代理 、 动态路由 、 请求转发等功能。Zuul  大部分功能都是通过过滤器实现的，Zuul  中定义了四种标准的过滤器类型，同时，还支持自定义过滤器

**Zuul的生命周期：**

pre ：在请求被路由之前调用
route ：在路由请求时被调用
post在 ：在 route 和 和 error  过滤器之后被调用，error

自定义两个过滤器来计算请求处理的时间：

```java
@Slf4j
@Component
public class PreRequestFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        // 这一次请求的上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set("startTime", System.currentTimeMillis());
        return null;
    }
}
```

```java
@Slf4j
@Component
public class AccessLogFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        Long startTime = (Long) context.get("startTime");
        String uri = request.getRequestURI();
        long duration = System.currentTimeMillis() - startTime;

        log.info("uri: " + uri + ", duration: " + duration / 100 + "ms");

        return null;
    }
}
```

**通用模块设计的思想与实现的功能**
设计思想：通用的代码、配置不应该散落在各个业务模块中，不利于维护与更新
一个大的系统，响应对象需要统一外层格式
各种业务设计与实现，可能会抛出各种各样的异常，异常信息的收集也应该做到统一

统一响应处理开发：

```java
@RestControllerAdvice
public class CommonResponseDataAdvice implements ResponseBodyAdvice<Object> {

    @Override
    @SuppressWarnings("all")
    public boolean supports(MethodParameter methodParameter,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        // 被@IgnoreResponseAdvice注解标识的类和方法不作响应
        if (methodParameter.getDeclaringClass().isAnnotationPresent(
                IgnoreResponseAdvice.class)) {
            return false;
        }
        if (methodParameter.getMethod().isAnnotationPresent(
                IgnoreResponseAdvice.class)) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("all")
    public Object beforeBodyWrite(@Nullable Object o,
                                  MethodParameter methodParameter,
                                  MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass,
                                  ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {
        CommonResponse<Object> response = new CommonResponse<>(0, "");
        if (null == o) {
            return response;
        } else if (o instanceof CommonResponse) {
            response = (CommonResponse<Object>) o;
        } else {
            response.setData(o);
        }
        return response;
    }
}
```

统一的异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(value = AdException.class)
    public CommonResponse<String> handlerAdException(HttpServletRequest req,
                                                     AdException ex) {
        CommonResponse<String> response = new CommonResponse<>(-1,
                "business error");
        response.setData(ex.getMessage());
        return response;
    }
}
```

统一配置的开发：

```java
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>>
                                                       converters) {
        converters.clear();
        // JAVA对象转换为JSON对象的转换器
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
```

工具类（MD5加密和时间类型解析）：

```java
import com.zhou.ad.exception.AdException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateUtils;

import java.util.Date;

public class CommonUtils {

    private static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd"
    };

    public static String md5(String value) {

        return DigestUtils.md5Hex(value).toUpperCase();
    }

    public static Date parseStringDate(String dateString)
            throws AdException {

        try {
            return DateUtils.parseDate(dateString, parsePatterns);
        } catch (Exception ex) {
            throw new AdException(ex.getMessage());
        }
    }
}
```

**广告投放数据的核心要素**

用户账户 > ->  最高层级，用于定义广告主或代理商，只有有了用户才会有接下来的数据投放

推广计划 > ->  一类品牌或产品广告投放的规划 ， 自身并不定义太多关于广告自身的信息 ， 它会将信息打包下放到推广单元层级

推广单元 > ->  一个确定的广告投放策略，描述了投放广告的规则信息

推广单元维度限制 > -> 等 广告投放会有一些限制条件，例如只投放到北京、上海地区，对一些关键字进行投放等

广告创意 > -> 展示给用户看到的数据，可以是图片、文本或者一段视频

投放系统是比较简单的模块，其核心实现的功能就是对广告数据（各个表）进行增删改查，即能够让用户（广告主/代理商）对数据进行查看、上传、修改与删除

**广告数据索引的设计**
设计索引的目的就是为了加快检索的速度 ， 将原始数据抽象 ， 规划出合理的字段 ， 在内存中构建广告数据索引 记住 ， 并不是所有的数据都需要放在索引里