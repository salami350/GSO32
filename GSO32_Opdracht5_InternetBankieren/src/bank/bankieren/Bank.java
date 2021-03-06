package bank.bankieren;

import ObserverObserverable.PropertyListener;
import fontys.util.*;
import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;
import java.util.*;

public class Bank implements IBank {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8728841131739353765L;
	private Map<Integer,IRekeningTbvBank> accounts;
	private Collection<IKlant> clients;
	private int nieuwReknr;
	private String name;
        
        private Collection<PropertyListener> listeners;

	public Bank(String name) {
		accounts = new HashMap<Integer,IRekeningTbvBank>();
		clients = new ArrayList<IKlant>();
		nieuwReknr = 100000000;	
		this.name = name;	
                
                listeners = new ArrayList<>();
	}

	public synchronized int openRekening(String name, String city) {
		if (name.equals("") || city.equals(""))
			return -1;

		IKlant klant = getKlant(name, city);
		IRekeningTbvBank account = new Rekening(nieuwReknr, klant, Money.EURO, this);
		accounts.put(nieuwReknr,account);
		nieuwReknr++;
		return nieuwReknr-1;
	}

	private IKlant getKlant(String name, String city) {
		for (IKlant k : clients) {
			if (k.getNaam().equals(name) && k.getPlaats().equals(city))
				return k;
		}
		IKlant klant = new Klant(name, city);
		clients.add(klant);
		return klant;
	}

	public IRekening getRekening(int nr) {
		return accounts.get(nr);
	}

	public synchronized boolean maakOver(int source, int destination, Money money)
			throws NumberDoesntExistException {
		if (source == destination)
			throw new RuntimeException(
					"cannot transfer money to your own account");
		if (!money.isPositive())
			throw new RuntimeException("money must be positive");

		IRekeningTbvBank source_account = (IRekeningTbvBank) getRekening(source);
		if (source_account == null)
			throw new NumberDoesntExistException("account " + source
					+ " unknown at " + name);

		Money negative = Money.difference(new Money(0, money.getCurrency()),
				money);
		boolean success = source_account.muteer(negative);
		if (!success)
			return false;

		IRekeningTbvBank dest_account = (IRekeningTbvBank) getRekening(destination);
		if (dest_account == null) 
			throw new NumberDoesntExistException("account " + destination
					+ " unknown at " + name);
		success = dest_account.muteer(money);

		if (!success) // rollback
			source_account.muteer(money);
		return success;
	}

	@Override
	public String getName() {
		return name;
	}
        
            
    /**
     * informt alle listeners dat een verandering is
     * @param update
     * @param text
     * @param oldVal
     * @param newVal
    */
    @Override
    public void inform(IRekening update, String text, Object oldVal, Object newVal) {
        for(PropertyListener pl : listeners) {
            pl.PropertyChange(new PropertyChangeEvent(update, text, oldVal, newVal));
        }
    }

    @Override
    public void addListener(PropertyListener pl, String s) {
        listeners.add(pl);
    }

    @Override
    public void removeListener(PropertyListener pl, String s) {
        listeners.remove(pl);
    }
}
