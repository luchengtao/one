package demo;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.nsn.logger.Logger;
import com.nsn.web.lte.DoSystem;
import com.nsn.web.lte.DoWebApplication;
import com.nsn.web.lte.mvc.Actions;

public class demoActivator implements BundleActivator {
	private Logger log = Logger.getLogger(this.getClass());
	@Override
	public void start(BundleContext context) throws Exception {
		DoWebApplication webapp = new DoWebApplication(DoSystem.getWebSystem(), context.getBundle(),
				this.getClass().getClassLoader());
		webapp.setContextPath("/my");
		Actions.add("/my/demo", DemoAction.class);
		
		
		
		DoSystem.getWebSystem().addWebApplication(webapp);
		log.info("=======demo");
	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
