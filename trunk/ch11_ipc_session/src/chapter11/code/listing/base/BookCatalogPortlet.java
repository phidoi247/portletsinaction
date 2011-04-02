package chapter11.code.listing.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.ProcessAction;
import javax.portlet.RenderMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ValidatorException;
import javax.portlet.WindowState;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.portlet.PortletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import chapter11.code.listing.domain.Book;
import chapter11.code.listing.service.BookService;
import chapter11.code.listing.service.BookServiceImpl;
import chapter11.code.listing.utils.Constants;

/**
 * BookCatalogPortlet class represents the portlet class responsible for
 * handling portlet requests.
 * 
 * @author asarin
 */
public class BookCatalogPortlet extends GenericPortlet {

	// -- represents the maximum size of the uploaded file = 1 MB
	private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024;
	private Logger logger = Logger.getLogger(BookCatalogPortlet.class);
	private static BookService bookService;

	/*
	 * Overrides the init method of the GenericPortlet class to set the
	 * BookService. BookService is simply a service which retrieves data from
	 * the PortletContext. The data was put into the PortletContext by
	 * BookCatalogContextListener, configured in web.xml file.
	 * 
	 * @see javax.portlet.GenericPortlet#init()
	 */
	public void init() {
		bookService = new BookServiceImpl(this.getPortletContext());
	}

	public static BookService getBookService() {
		return bookService;
	}
	
	/*
	 * doHeaders method is responsible for adding bookCatalog.css (CSS file) and
	 * to the <head> section of the HTML markup
	 * generated by the portal page. This is an optional feature and may not be
	 * available in all portal servers. For example, JetSpeed allows you to add
	 * elements, and also Liferay with OpenPortal Portlet Container. Glassfish
	 * v2.1 ignores the MARKUP_HEAD_ELEMENT header.
	 * 
	 * @see javax.portlet.GenericPortlet#doHeaders(javax.portlet.RenderRequest,
	 * javax.portlet.RenderResponse)
	 */
	protected void doHeaders(RenderRequest request, RenderResponse response) {
		super.doHeaders(request, response);
		PortalContext portalContext = request.getPortalContext();
		String portalInfo = portalContext.getPortalInfo();

		// -- adding DOM element to head is supported by JetSpeed 2.2
		if (portalInfo.contains(Constants.JETSPEED)) {
			// -- add CSS
			Element cssElement = response.createElement("link");
			// --encoding URLs is important
			cssElement.setAttribute("href", response.encodeURL((request
					.getContextPath() + "/css/bookCatalog.css")));
			cssElement.setAttribute("rel", "stylesheet");
			cssElement.setAttribute("type", "text/css");
			response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, cssElement);

			// -- add JavaScript
			Element jsElement = response.createElement("script");

			// --encoding URLs to resources is important
			jsElement.setAttribute("src", response.encodeURL((request
					.getContextPath() + "/js/bookCatalog.js")));
			jsElement.setAttribute("type", "text/javascript");
			response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, jsElement);
		}
	}

	/**
	 * Render method that is invoked when the portlet mode is 'print'. If the
	 * portal server doesn't support this mode then there is simply ignored. For
	 * example, JetSpeed 2.2 and Liferay 5.2.3 support 'print' custom mode.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws PortletException
	 */
	@RenderMode(name = "print")
	public void showPrint(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
		logger.info("Generating printable version of catalog");
		request.setAttribute("books", bookService.getBooks());
		getPortletContext().getRequestDispatcher(
				response.encodeURL(Constants.PATH_TO_JSP_PAGE
						+ "printCatalog.jsp")).include(request, response);
	}

	/**
	 * Render method for HELP portlet mode. In this mode portlet shows help
	 * information to the user.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws PortletException
	 */
	@RenderMode(name = "help")
	public void showHelp(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
		logger.info("Generating Help information for the portlet");
		String titleKey = "portlet.title.help";
		response.setTitle(getResourceBundle(request.getLocale()).getString(
				titleKey));
		getPortletContext().getRequestDispatcher(
				response.encodeURL(Constants.PATH_TO_JSP_PAGE + "help.jsp"))
				.include(request, response);
	}

	/**
	 * Render method for the EDIT portlet mode. In this mode the portlet allows
	 * users to view and specify their preferences. The Book catalog portlet
	 * in-turn personalizes portlet content / behavior based on the preferences
	 * selected/entered by the user.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws PortletException
	 */
	@RenderMode(name = "edit")
	public void showPrefs(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
		logger.info("Generating Preferences details for the portlet");
		String titleKey = "portlet.title.preferences";
		response.setTitle(getResourceBundle(request.getLocale()).getString(
				titleKey));
		getPortletContext().getRequestDispatcher(
				response.encodeURL(Constants.PATH_TO_JSP_PAGE
						+ "preferences.jsp")).include(request, response);
	}

	/**
	 * Render method for the VIEW portlet mode. This is where all the main
	 * business functionality of the portlet lies.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws PortletException
	 */
	@SuppressWarnings("unchecked")
	@RenderMode(name = "VIEW")
	public void showBooks(RenderRequest request, RenderResponse response)
			throws IOException, PortletException {
		logger.info("Inside showBooks method");

		// -- for information purpose, the portlet prints
		// -- the supported portlet modes and window
		// --states
		PortalContext context = request.getPortalContext();
		printSupportedPortletModes(context);
		printSupportedWindowStates(context);


		// --get user attributes user.name.given and user.name.family
		// --these attributes must be defined in the portlet.xml file to
		// --allow portlet container/server to make them available to the
		// --portlet
		Map<String, Object> userAttributeMap = (Map<String, Object>) request
				.getAttribute(PortletRequest.USER_INFO);
		String firstName = "";
		String lastName = "";
		if (userAttributeMap != null) {
			firstName = (String) userAttributeMap.get("user.name.given");
			lastName = (String) userAttributeMap.get("user.name.family");
			// --set firstName and lastName in request so that JSP can pick
			// --from the request
			request.setAttribute("firstName", firstName);
			request.setAttribute("lastName", lastName);
		}

		// -- the portal info is used by the portlet to display information
		// -- about the portal server on which it is currently deployed
		String portalInfo = context.getPortalInfo();
		request.setAttribute("portalInfo", portalInfo);

		// -- Property in the resource bundle which identifies the current
		// portlet title
		String titleKey = "";

		// --myaction identifies the current action which is taking place in the
		// portlet
		String myaction = request.getParameter(Constants.MYACTION_PARAM);

		// --dynamically obtain the title for the portlet, based on myaction
		// parameter
		if (myaction == null || "".equals(myaction)) {
			titleKey = "portlet.title.showCatalog";
		} else {
			titleKey = "portlet.title."
					+ request.getParameter(Constants.MYACTION_PARAM);
		}
		response.setTitle(getResourceBundle(request.getLocale()).getString(
				titleKey));

		// --dispatch request to the appropriate JSP page based on the value of
		// --myaction request parameter
		String jspPage = "error.jsp";
		if (myaction == null || "showCatalog".equalsIgnoreCase(myaction)
				|| "".equals(myaction)) {
			List<Book> books = new ArrayList<Book>();
			//--obtain caregory and preferred book ISBN preferences for the user
			PortletPreferences prefs = request.getPreferences();
			String[] categories = prefs.getValues("category", new String[] {"-99"});
			//-- convert prefIsbnNumbers String[] to a list for ease of comparison
			List<String> prefIsbnNumbers = Arrays.asList(prefs.getValues("prefBookISBN", new String[] {"-99"}));

			if(categories != null && categories.length == 1 && categories[0].equals("-99")) {
				//--get books from all categories
				books = bookService.getBooks();
			} else {
				//--get books from the user's preferred categories
				books = bookService.getBooksByCategories(categories);
			}
			for(Book book : books) {
				if(prefIsbnNumbers.contains(book.getIsbnNumber().toString())) {
					logger.info("Book '" + book.getName() + "' is a preferred book");
					book.setPreferredBook(true);
				} else {
					book.setPreferredBook(false);
				}
			}
			request.setAttribute(Constants.BOOKS_ATTR, books);
			jspPage = "home.jsp";
		}
		if ("showSearchResults".equalsIgnoreCase(myaction)) {
			jspPage = "home.jsp";
		}
		if ("uploadTocForm".equalsIgnoreCase(myaction)) {
			jspPage = "uploadForm.jsp";
		}
		if ("addBookForm".equalsIgnoreCase(myaction)) {
			jspPage = "addBookForm.jsp";
		}
		if ("error".equalsIgnoreCase(myaction)) {
			request.setAttribute("exceptionMsg", request
					.getParameter("exceptionMsg"));
			jspPage = "error.jsp";
		}
		if ("addBookForm".equalsIgnoreCase(myaction)) {
			jspPage = "addBookForm.jsp";
		}
		if ("refreshResults".equalsIgnoreCase(myaction)) {
			String bookNameSearchField = (String) request.getPortletSession()
					.getAttribute("bookNameSearchField");
			String authorNameSearchField = (String) request.getPortletSession()
					.getAttribute("authorNameSearchField");
			if (bookNameSearchField == null) {
				bookNameSearchField = "";
			}
			if (authorNameSearchField == null) {
				authorNameSearchField = "";
			}
			logger.info("Searching for books with name : "
					+ bookNameSearchField + " and author name : "
					+ authorNameSearchField);
			PortletPreferences prefs = request.getPreferences();
			String searchTypePref = prefs.getValue("searchType", Constants.CASE_SENSITIVE);
			List<Book> matchingBooks = bookService.searchBooks(
					bookNameSearchField, authorNameSearchField, searchTypePref);
			
			//-- mark preferred books
			List<String> prefIsbnNumbers = Arrays.asList(prefs.getValues("prefBookISBN", new String[] {"-99"}));
			for(Book book : matchingBooks) {
				if(prefIsbnNumbers.contains(book.getIsbnNumber().toString())) {
					logger.info("Book '" + book.getName() + "' is a preferred book");
					book.setPreferredBook(true);
				} else {
					book.setPreferredBook(false);
				}
			}			
			request.setAttribute(Constants.BOOKS_ATTR, matchingBooks);
			jspPage = "home.jsp";
		}
		getPortletContext().getRequestDispatcher(
				response.encodeURL(Constants.PATH_TO_JSP_PAGE + jspPage))
				.include(request, response);
	}

	/**
	 * Removes a book from the catalog.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "removeBookAction")
	public void removeBook(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside removeBook action method");
		bookService
				.removeBook(Long.valueOf(request.getParameter("isbnNumber")));
		response.setRenderParameter(Constants.MYACTION_PARAM, "showCatalog");
	}

	/**
	 * Resets a preference, identified by prefName request parameter.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "resetPreference")
	public void resetPreference(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside resetPreferences action method");
		PortletPreferences prefs = request.getPreferences();
		prefs.reset(request.getParameter("prefName"));
		prefs.store();
	}

	/**
	 * Saves preferences in the persistent store. If ValidatorException occurs
	 * then the method retrieves the default message corresponding to the
	 * preference key from resource bundle and adds it to the request
	 * attribute.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "savePreferences")
	public void savePreferences(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside savePreferences action method");
		//--map that holds error messages corresponding to the failed preference keys
		List<String> errorMessages = new ArrayList<String>();
		PortletPreferences prefs = request.getPreferences();
		
		//--retrieve selected preferences from the incoming portlet request
		String[] prefCategories = request.getParameterValues("prefCategory");
		String searchTypePref = request.getParameter("searchType");
		String maxNumOfBooksPref = request.getParameter("maxNumOfBooks");
		String isbnNumberPref = request.getParameter("prefBookISBN");
		
		//--preferences that were earlier saved by the user
		String[] savedPrefBookISBNNumbers = prefs.getValues("prefBookISBN", new String[] {"-99"});
		
		if (prefCategories != null) {
			prefs.setValues("category", prefCategories);
		}
		if (searchTypePref != null && !searchTypePref.equals("-1")) {
			prefs.setValue("searchType", searchTypePref);
		}
		if (maxNumOfBooksPref != null) {
			prefs.setValue("maxNumOfBooks", maxNumOfBooksPref);
		}
		//--add the preferred ISBN number key to the existing set of ISBN numbers
		if (isbnNumberPref != null && !isbnNumberPref.equals("")) {
			if(savedPrefBookISBNNumbers.length == 1 && savedPrefBookISBNNumbers[0].equals("-99")) {
				prefs.setValue("prefBookISBN", isbnNumberPref);
			} else {
				//-- add all preferences to the newPrefsList
				//--String[] newPrefsList = Arrays.copyOf(savedPrefBookISBNNumbers, savedPrefBookISBNNumbers.length + 1);
				String[] newPrefsList = new String[savedPrefBookISBNNumbers.length + 1];
				System.arraycopy(savedPrefBookISBNNumbers, 0, newPrefsList, 0, savedPrefBookISBNNumbers.length + 1);
				newPrefsList[savedPrefBookISBNNumbers.length] = isbnNumberPref;
				prefs.setValues("prefBookISBN", newPrefsList);
			}
		}
		try {
			prefs.store();
		} catch (ValidatorException e) {
			Enumeration<String> failedKeys = e.getFailedKeys();
			while(failedKeys.hasMoreElements()) {
				String failedKey = failedKeys.nextElement();
				logger.info("Failed key : " + failedKey);
				String errorMessage = getResourceBundle(request.getLocale()).getString(
						Constants.PREF_RESOURCE_IDENTIFIER_PREFIX + "." + failedKey + ".error");
				String prefName = getResourceBundle(request.getLocale()).getString(
						Constants.PREF_RESOURCE_IDENTIFIER_PREFIX + "." + failedKey + ".name");
				errorMessages.add(errorMessage.replace("{0}", prefName));
			}  
		}
		catch(Exception e) {
			errorMessages.add("Unexpected exception ocurred : Cause : " + e.getCause() + ". Message : " + e.getMessage());
			request.setAttribute("errorMessages", errorMessages);
		}
		request.setAttribute("errorMessages", errorMessages);
	}

	/**
	 * Uploads book's TOC. The TOC is uploaded to the folder identified
	 * by uploadFolder portlet initialization parameter.
	 * Method makes use of Commons FileUpload library to upload files.
	 * Make sure that you are using Commons FileUpload 1.1 or later.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "uploadTocAction")
	public void uploadToc(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside uploadToc action method");
		PortletFileUpload pfu = new PortletFileUpload();
		pfu.setFileSizeMax(MAX_UPLOAD_FILE_SIZE);
		String fileExtension = null;
		FileOutputStream outStream = null;
		try {
			FileItemIterator iter = pfu.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String fileName = item.getName();
				fileExtension = fileName.substring(fileName.lastIndexOf("."),
						fileName.length());

				outStream = new FileOutputStream(
						getInitParameter("uploadFolder") + "\\"
								+ request.getParameter("isbnNumber")
								+ fileExtension);
				InputStream stream = item.openStream();
				if (!item.isFormField()) {
					byte[] buffer = new byte[1024];
					while (true) {
						int bytes = stream.read(buffer);
						if (bytes <= 0) {
							break;
						}
						outStream.write(buffer, 0, bytes);
					}
					outStream.flush();
				}
			}
			response
					.setRenderParameter(Constants.MYACTION_PARAM, "showCatalog");
		} catch (Exception ex) {
			// --close the output stream and delete the generated file
			if (outStream != null) {
				outStream.close();
			}
			File file = new File(getInitParameter("uploadFolder") + "\\"
					+ request.getPortletSession().getAttribute("isbnNumber")
					+ fileExtension);
			if (file != null && file.canRead() && file.canWrite()) {
				file.delete();
			}
			response.setRenderParameter(Constants.MYACTION_PARAM, "error");
			response
					.setRenderParameter(
							"exceptionMsg",
							"Exception occurred while uploading the file. Please check if you selected a file and its size is <= 1 MB");
		}
	}

	/**
	 * Resets the search results. When search is made, the matching results are shown.
	 * This method is invoked when the user clicks the 'Reset' hyperlink on the search
	 * results page.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "resetAction")
	public void resetAction(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside reset action");
		response.setRenderParameter(Constants.MYACTION_PARAM, "showCatalog");
	}

	/**
	 * Searches for a matching book. 
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "searchBookAction")
	public void searchBook(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("Inside search Book action");
		logger.info("Searching for books with name : "
				+ request.getParameter("bookNameSearchField")
				+ " and author name : "
				+ request.getParameter("authorNameSearchField"));
		// --store the search criteria in session
		request.getPortletSession().setAttribute("authorNameSearchField",
				request.getParameter("authorNameSearchField"));
		request.getPortletSession().setAttribute("bookNameSearchField",
				request.getParameter("bookNameSearchField"));
		// -- search books
		List<Book> matchingBooks = null;
		if (request.getParameter("bookNameSearchField") != null
				&& request.getParameter("authorNameSearchField") != null) {
			PortletPreferences prefs = request.getPreferences();
			String searchTypePref = prefs.getValue("searchType", Constants.CASE_SENSITIVE);
			matchingBooks = bookService.searchBooks(request
					.getParameter("bookNameSearchField"), request
					.getParameter("authorNameSearchField"), searchTypePref);
			
			//--mark preferred books
			List<String> prefIsbnNumbers = Arrays.asList(prefs.getValues("prefBookISBN", new String[] {"-99"}));
			for(Book book : matchingBooks) {
				if(prefIsbnNumbers.contains(book.getIsbnNumber().toString())) {
					logger.info("Book '" + book.getName() + "' is a preferred book");
					book.setPreferredBook(true);
				}
			}
		}
		logger.info("Number of matching books found : " + matchingBooks.size());
		// -- myaction attribute is used to show the 'Reset' link after the
		// search results
		// -- are shown to the user
		request.setAttribute(Constants.MYACTION_PARAM, "showSearchResults");
		request.setAttribute(Constants.BOOKS_ATTR, matchingBooks);
		response.setRenderParameter(Constants.MYACTION_PARAM,
				"showSearchResults");
	}

	/**
	 * Adds a book to the catalog.
	 * 
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "addBookAction")
	public void addBook(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		logger.info("addBook action invoked");
		String category = request.getParameter("category");
		String name = request.getParameter("name");
		String author = request.getParameter("author");
		String isbnNumber = request.getParameter("isbnNumber");

		logger.info("Book name: " + name + ", author : " + author
				+ ", ISBN number: " + isbnNumber);

		// --contains map of field names to error message
		Map<String, String> errorMap = new HashMap<String, String>();

		if (category == null || category.trim().equalsIgnoreCase("")) {
			errorMap.put("category", "Please enter category");
		}
		if (name == null || name.trim().equalsIgnoreCase("")) {
			errorMap.put("name", "Please enter book name");
		}
		if (author == null || author.trim().equalsIgnoreCase("")) {
			errorMap.put("author", "Please enter author name");
		}
		if (isbnNumber == null || isbnNumber.trim().equalsIgnoreCase("")) {
			errorMap.put("isbnNumber", "Please enter ISBN number");
		}
		if (isbnNumber == null || !StringUtils.isNumeric(isbnNumber)) {
			errorMap.put("isbnNumber", "Please enter a valid ISBN number");
		}

		// --if no error found, go ahead and save the book
		if (errorMap.isEmpty()) {
			logger.info("adding book to the data store");
			bookService.addBook(new Book(category, name, author, Long
					.valueOf(isbnNumber)));
			response
					.setRenderParameter(Constants.MYACTION_PARAM, "showCatalog");
			//-- now that the Book is successfully saved. Lets save the information
			//-- in PortletSession and will be used by Recently Added Book portlet
			request.getPortletSession().setAttribute("recentBookIsbn", isbnNumber, PortletSession.APPLICATION_SCOPE);
			logger.info("added book to the PortletSession");
		} else {
			logger
					.info("validation error occurred. re-showing the add book form");
			request.setAttribute("errors", errorMap);
			// -- contains property name to property value map, for re-rendering
			// the form
			// -- with values that were entered by the user for each form field
			Map<String, String> valuesMap = new HashMap<String, String>();
			valuesMap.put("name", name);
			valuesMap.put("author", author);
			valuesMap.put("isbnNumber", isbnNumber);
			valuesMap.put("category", category);
			
			// --set the valuesMap as 'book' request attribute. JSP page
			// --will use this to show the value of properties in case form
			// --validation fails. In case of JetSpeed 2.2 this attribute will
			// not be able to make it to the following render method because
			// currently it doesn't support actionScopedRequestAttributes
			request.setAttribute("book", valuesMap);
			response
					.setRenderParameter(Constants.MYACTION_PARAM, "addBookForm");
		}
	}

	//-- Print supported portlet modes by the portal server
	private void printSupportedPortletModes(PortalContext context) {
		// -- supported portlet modes by the portal server
		Enumeration<PortletMode> portletModes = context
				.getSupportedPortletModes();
		while (portletModes.hasMoreElements()) {
			PortletMode mode = portletModes.nextElement();
			logger.info("Support portlet mode " + mode.toString());
		}
	}

	//-- prints support window states by the portal server
	private void printSupportedWindowStates(PortalContext context) {
		// -- supported window states by the portal server
		Enumeration<WindowState> windowStates = context
				.getSupportedWindowStates();
		while (windowStates.hasMoreElements()) {
			WindowState windowState = windowStates.nextElement();
			logger.info("Support window state " + windowState.toString());
		}
	}
}
