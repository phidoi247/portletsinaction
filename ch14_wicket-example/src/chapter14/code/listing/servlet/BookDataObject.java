package chapter14.code.listing.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chapter14.code.listing.Book;

public class BookDataObject {
	private List<Book> books = Collections.synchronizedList(new ArrayList<Book>());
	
	public BookDataObject() {
		books.add(new Book("AspectJ in Action, Second Edition", "Ramnivas Laddad", Long.valueOf("1933988053")));
		books.add(new Book("ActiveMQ in Action", "Bruce Snyder, Dejan Bosanac, and Rob Davies", Long.valueOf("1933988940")));
		books.add(new Book("Hadoop in Action", "Chuck Lam", Long.valueOf("9781935182191")));
		books.add(new Book("JUnit in Action, Second Edition", "Petar Tahchiev, Felipe Leme, Vincent Massol, and Gary Gregory", Long.valueOf("9781935182023")));
		books.add(new Book("Griffon in Action", "Andres Almiray and Danno Ferrin", Long.valueOf("9781935182238")));
		books.add(new Book("Lift in Action", "Timothy Perrett", Long.valueOf("9781935182801")));
		books.add(new Book("OSGi in Action", "Richard S. Hall, Karl Pauls, Stuart McCulloch, and David Savage", Long.valueOf("1933988916")));
	}
	
	public List<Book> getBooks() {
		return books;
	}
}
