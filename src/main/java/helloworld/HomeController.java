package helloworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @Autowired(required = false) DataSource dataSource;
    @Autowired(required = false) RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false) ApplicationInstanceInfo instanceInfo;

    @RequestMapping("/")
    public String home(Model model) {
        model.addAttribute("instanceInfo", instanceInfo);

        if (instanceInfo != null) {
            Map<Class<?>, String> services = new LinkedHashMap<Class<?>, String>();
            services.put(dataSource.getClass(), toString(dataSource));
            services.put(redisConnectionFactory.getClass(), toString(redisConnectionFactory));
            model.addAttribute("services", services.entrySet());
        }

        return "home";
    }

    @RequestMapping("/kill")
    public void kill() {
        System.out.println("***** KILL VM *********************");
        System.exit(0);
    }

    private String toString(DataSource dataSource) {
        if (dataSource == null) {
            return "<none>";
        } else {
            try {
                Field urlField = ReflectionUtils.findField(dataSource.getClass(), "url");
                ReflectionUtils.makeAccessible(urlField);
                return stripCredentials((String) urlField.get(dataSource));
            } catch (Exception fe) {
                try {
                    Method urlMethod = ReflectionUtils.findMethod(dataSource.getClass(), "getUrl");
                    ReflectionUtils.makeAccessible(urlMethod);
                    return stripCredentials((String) urlMethod.invoke(dataSource, (Object[])null));
                } catch (Exception me){
                    return "<unknown> " + dataSource.getClass();
                }
            }
        }
    }

    private String toString(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return "<none>";
        } else {
            if (redisConnectionFactory instanceof JedisConnectionFactory) {
                JedisConnectionFactory jcf = (JedisConnectionFactory) redisConnectionFactory;
                return jcf.getHostName().toString() + ":" + jcf.getPort();
            } else if (redisConnectionFactory instanceof LettuceConnectionFactory) {
                LettuceConnectionFactory lcf = (LettuceConnectionFactory) redisConnectionFactory;
                return lcf.getHostName().toString() + ":" + lcf.getPort();
            }
            return "<unknown> " + redisConnectionFactory.getClass();
        }
    }

    private String stripCredentials(String urlString) {
        try {
            if (urlString.startsWith("jdbc:")) {
                urlString = urlString.substring("jdbc:".length());
            }
            URI url = new URI(urlString);
            return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), null, null).toString();
        }
        catch (URISyntaxException e) {
            System.out.println(e);
            return "<bad url> " + urlString;
        }
    }

}
