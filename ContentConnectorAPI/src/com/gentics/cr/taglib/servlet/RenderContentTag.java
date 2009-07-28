package com.gentics.cr.taglib.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Hashtable;

import javax.portlet.RenderRequest;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import com.gentics.api.portalnode.connector.PLinkReplacer;
import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.CRException;
import com.gentics.cr.CRResolvableBean;
import com.gentics.cr.configuration.GenericConfiguration;
import com.gentics.cr.rendering.ContentRenderer;
import com.gentics.cr.rendering.contentprocessor.ContentPostProcesser;

/**
 * @author norbert
 * Implementation of a tag that renders content with plink replacing and velocity
 */
public class RenderContentTag extends TagSupport {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5724484220477278975L;
	private Logger logger = Logger.getLogger("com.gentics.cr.rendering");

	/**
	 * Name of the render request attribute for the instance of {@link ContentRenderer}
	 */
	public final static String RENDERER_PARAM = "rendercontenttag.renderer";
	
	/**
	 * Name of the config attribute for the instance of {@link GenericConfiguration}
	 */
	public final static String CRCONF_PARAM = "rendercontenttag.crconf";

	/**
	 * Name of the request attribute for the instance of {@link RenderRequest}
	 */
	public final static String REQUEST_PARAM = "rendercontenttag.request";
	
	
	/**
	 * Name of the render request attribute for the instance of {@link PLinkReplacer}
	 */
	public final static String PLINK_PARAM = "rendercontenttag.plinkreplacer";

	/**
	 * Rendered object
	 */
	protected CRResolvableBean object;
	public static final String SESSION_KEY_CONTENTPOSTPROCESSOR_CONF = RenderContentTag.class.getName() + "|ContentPostProcessor|confs";

	/**
	 * name of the rendered attribute
	 */
	protected String contentAttribute = "content";
	
	protected String var = null;

	/**
	 * flag if the output should be urlencoded
	 */
	protected boolean urlencode = false;

	/**
	 * Set the object to be rendered. Must be an instance of {@link CRResolvableBean}.
	 * @param object rendered object
	 */
	public void setObject(Object object) {
		if (object instanceof CRResolvableBean) {
			this.object = (CRResolvableBean) object;
		}
	}

	/**
	 * Set the content attribute to be rendered
	 * @param contentAttribute name of the rendered content attribute
	 */
	public void setContentAttribute(String contentAttribute) {
		this.contentAttribute = contentAttribute;
	}
	
	/**
	 * Set the flag if the returned content should be url-encoded
	 * @param urlencode 
	 * 
	 */
	public void setUrlencode(String urlencode) {
		this.urlencode = "true".equals(urlencode);
	}
	
	/**
	 * 
	 * @param var
	 */
	public void setVar(String var) {
		this.var = var;
	}
	
	

	/**
	 * @return 
	 * @throws JspException 
	 * 
	 */
	public int doEndTag() throws JspException {
		// get the ContentRenderer
		ServletRequest renderRequest = getServletRequest();
		HttpSession session = pageContext.getSession();

		ContentRenderer renderer = (ContentRenderer)renderRequest.getAttribute(RENDERER_PARAM);
		PLinkReplacer pLinkReplacer = (PLinkReplacer)renderRequest.getAttribute(PLINK_PARAM);
		CRConfigUtil crConf = (CRConfigUtil)renderRequest.getAttribute(CRCONF_PARAM);
		
		
		try {
			if (object != null) {
				try {
					String content = renderer.renderContent(object, contentAttribute, true, pLinkReplacer, false, null);
					
					/* Get the ContentPostProcessor-Config from the PortletSession or instance it from the Config*/
					@SuppressWarnings("unchecked")
					Hashtable<String,ContentPostProcesser> confs = (Hashtable<String, ContentPostProcesser>) session.getAttribute(SESSION_KEY_CONTENTPOSTPROCESSOR_CONF);
					if (confs == null){
						confs = ContentPostProcesser.getProcessorTable(crConf);
						session.setAttribute(SESSION_KEY_CONTENTPOSTPROCESSOR_CONF, confs);
						logger.debug("Put ContentPostProcessor config into session of servlet " + session.getServletContext().getServletContextName() + "!");
					}
					if (confs != null) {
						for(ContentPostProcesser p:confs.values()) {
							content = p.processString(content, renderRequest);
						}
					}
					
					if (urlencode) {
						content = URLEncoder.encode(content, "UTF-8");
					}
					
					if (var != null) {
						if (content!=null && "".equals(content)) {
							content = null;
						}
						pageContext.setAttribute(var, content, PageContext.REQUEST_SCOPE);
					} else {
						pageContext.getOut().write(content);
					}
					
				} catch (CRException e) {
					throw new JspException("Error while rendering object "
							+ object.getContentid(), e);
				}
			} else {
				pageContext.getOut().write(" -- no object set --");
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return super.doEndTag();
	}

	/**
	 * Get the servlet request
	 * 
	 * @return servlet request
	 * @throws JspException
	 *             when the servlet request could not be found
	 */
	protected ServletRequest getServletRequest() throws JspException {
		return(pageContext.getRequest());
	}

}
