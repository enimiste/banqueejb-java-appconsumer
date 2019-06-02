
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.nouni.tuto.banque.metier.AccountStoreRemote;

public class App implements Runnable {

	protected AccountStoreRemote metier;

	public App(AccountStoreRemote metier) {
		super();
		this.metier = metier;
	}

	public static void main(String[] args) throws NamingException {
		Properties props = new Properties();
		// props.put(Context.INITIAL_CONTEXT_FACTORY,
		// "com.sun.enterprise.naming.SerialInitContextFactory");
		// props.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
		// props.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

		Optional.ofNullable((AccountStoreRemote) (new InitialContext(props)).lookup("ejb/account-store-ejb"))
				.ifPresent(metier -> {
					(new App(metier)).run();
				});

	}

	@Override
	public void run() {
		//test1();
		try {
			interactif();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void interactif() throws Exception {
		ScannerReader<String> readString = (scn, r, msg, fn) -> {
			while(scn.hasNext()) {
				try {
					String str = scn.nextLine();
					if(str != null && !str.isEmpty()) {
						fn.apply(str);
						return;
					} else {
						System.out.print(msg + " :");
					}
				} catch(NoSuchElementException e) {
					System.out.print(msg + " :");
				} catch(IllegalStateException e) {
					System.out.print(msg + " :");
				}
			}
		};
		
		ScannerReader<Double> readDouble = (scn, r, msg, fn) -> {
			while(scn.hasNext()) {
				try {
					Double db = scn.nextDouble();
					fn.apply(db);
					return;
				} catch(NoSuchElementException e) {
					System.out.print(msg + " :");
				} catch(IllegalStateException e) {
					System.out.print(msg + " :");
				}
			}
		};
		
		Function<Object, Object> help = (o) -> {
			System.out.println("-------------------------------------------------------------------");
			System.out.println("| Welcome to our Banque Account manager App                        |");
			System.out.println("| This App comunicate with an EJB Session deployed on a App Server |");
			System.out.println("| Author : Ing. NOUNI EL Bachir                                    |");
			System.out.println("-------------------------------------------------------------------");
			System.out.println("1. List all accounts");
			System.out.println("2. Create a new account");
			System.out.println("3. Do a virement between two accounts");
			System.out.println("4. Find an account using its Ref");
			System.out.println("5. Show logs of an account given its Ref");
			System.out.println("6. List all accounts having the solde less than a value");
			System.out.println("10. To exit the app");
			System.out.print("Type your choice here :");
			return null;
		};
		
		Function<Scanner, Object> create = (s) -> {
			System.out.print("We need the initial solde to create a new account :");
			readDouble.read(s, sc->sc.nextDouble(), "Please type a correct number", solde-> {
				System.out.println("The new account has the ref : " + metier.create(solde));
			});
			return null;
		};
		
		Function<Scanner, Object> virer = (s) -> {
			System.out.print("From which account you need to do the transfert :");
			readString.read(s, sc->sc.nextLine(), "Please type a no empty ref.", source -> {
				System.out.print("To which account you need to do the transfert :");
				readString.read(s, sc->sc.nextLine(), "Please type a no empty ref.", dest -> {
					System.out.print("How much you like to transfert :");
					readDouble.read(s, sc->sc.nextDouble(), "Please type a correct montant", montant-> {
						metier.virer(source, dest, montant);
						System.out.println("Transfert done !");
					});
				});
			});
			return null;
		};
		
		Function<Scanner, Object> find = (s) -> {
			System.out.print("Type the ref of the account that you search for :");
			readString.read(s, sc->sc.nextLine(), "Please type a no empty ref.", ref-> {
				System.out.println(metier.findByRef(ref));
			});
			return null;
		};
		
		Function<Scanner, Object> logs = (s) -> {
			System.out.print("Type the ref of the account that you need logs for :");
			readString.read(s, sc->sc.nextLine(), "Please type a no empty ref.", ref-> {
				metier.findByRef(ref).getEvents().forEach(e -> System.out.println("  " + e));
			});
			return null;
		};
		
		Function<Scanner, Object> lessThan = (s) -> {
			System.out.print("Type the min amount that should have these accounts :");
			readDouble.read(s, sc->sc.nextDouble(), "Please type a correct amount.", montant-> {
				metier.withSoldeLessThan(montant).forEach(System.out::println);
			});
			return null;
		};
		
		help.apply(null);
		Scanner cmd = new Scanner(System.in);
		while(cmd.hasNext()) {
			switch(cmd.nextInt()) {
			case 10 : {
				System.out.println("Thanks for using our app");
				System.exit(0);
				return;
			}
			case 1 :
				metier.listAll().forEach(System.out::println);
				break;
			case 2 :
				create.apply(cmd);
				break;
			case 3 :
				virer.apply(cmd);
				break;
			case 4 :
				find.apply(cmd);
				break;
			case 5 :
				logs.apply(cmd);
				break;
			case 6 :
				lessThan.apply(cmd);
				break;
			default:
				System.out.println("Invalid choice.");
			}
			System.out.println("");
			help.apply(null);
		}
	}

	void test1() {
		String ref = metier.create(2500);
		metier.listAll().forEach(System.out::println);
		System.out.println("");
		Optional.ofNullable(metier.findByRef(ref)).ifPresent(System.out::println);
		String ref2 = metier.create(0);
		metier.listAll().forEach(System.out::println);
		metier.virer(ref, ref2, 1200);
		Optional.ofNullable(metier.findByRef(ref))
				.ifPresent(a -> a.getEvents().forEach(e -> System.out.println("\t" + e)));
		System.out.println("");
		Optional.ofNullable(metier.findByRef(ref2))
				.ifPresent(a -> a.getEvents().forEach(e -> System.out.println("\t" + e)));
		metier.listAll().forEach(System.out::println);
	}

}

@FunctionalInterface
interface ScannerReader<T> {
	public void read(Scanner s, Function<Scanner, Object> read, String msgOnError, NoArgs<T> fn);
}

@FunctionalInterface
interface NoArgs<T> {
	public void apply(T t);
}
