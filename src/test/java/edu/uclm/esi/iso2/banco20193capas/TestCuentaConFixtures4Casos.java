package edu.uclm.esi.iso2.banco20193capas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import edu.uclm.esi.iso2.banco20193capas.exceptions.CuentaInvalidaException;
import edu.uclm.esi.iso2.banco20193capas.exceptions.ImporteInvalidoException;
import edu.uclm.esi.iso2.banco20193capas.exceptions.PinInvalidoException;
import edu.uclm.esi.iso2.banco20193capas.exceptions.SaldoInsuficienteException;
import edu.uclm.esi.iso2.banco20193capas.exceptions.TarjetaBloqueadaException;
import edu.uclm.esi.iso2.banco20193capas.model.Cliente;
import edu.uclm.esi.iso2.banco20193capas.model.Cuenta;
import edu.uclm.esi.iso2.banco20193capas.model.Manager;
import edu.uclm.esi.iso2.banco20193capas.model.TarjetaCredito;
import edu.uclm.esi.iso2.banco20193capas.model.TarjetaDebito;
import junit.framework.TestCase;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestCuentaConFixtures4Casos extends TestCase {
	private Cuenta cuentaPepe, cuentaAna;
	private Cliente pepe, ana;
	private TarjetaDebito tdPepe, tdAna;
	private TarjetaCredito tcPepe, tcAna;
	
	@Before
	public void setUp() {
		Manager.getMovimientoDAO().deleteAll();
		Manager.getMovimientoTarjetaCreditoDAO().deleteAll();
		Manager.getTarjetaCreditoDAO().deleteAll();
		Manager.getTarjetaDebitoDAO().deleteAll();
		Manager.getCuentaDAO().deleteAll();
		Manager.getClienteDAO().deleteAll();
		
		this.pepe = new Cliente("12345X", "Pepe", "Pérez"); this.pepe.insert();
		this.ana = new Cliente("98765F", "Ana", "López"); this.ana.insert();
		this.cuentaPepe = new Cuenta(1); this.cuentaAna = new Cuenta(2);
		try {
			this.cuentaPepe.addTitular(pepe); this.cuentaPepe.insert(); this.cuentaPepe.ingresar(1000);
			this.cuentaAna.addTitular(ana); this.cuentaAna.insert(); this.cuentaAna.ingresar(5000);
			this.tcPepe = this.cuentaPepe.emitirTarjetaCredito(pepe.getNif(), 2000);
			this.tcAna = this.cuentaAna.emitirTarjetaCredito(ana.getNif(), 10000);
			this.tdPepe = this.cuentaPepe.emitirTarjetaDebito(pepe.getNif());
			this.tdAna = this.cuentaAna.emitirTarjetaDebito(ana.getNif());
			
			this.tcPepe.cambiarPin(tcPepe.getPin(), 1234);
			this.tcAna.cambiarPin(tcAna.getPin(), 1234);
			this.tdPepe.cambiarPin(tdPepe.getPin(), 1234);
			this.tdAna.cambiarPin(tdAna.getPin(), 1234);

		}
		catch (Exception e) {
			fail("Excepción inesperada en setUp(): " + e);
		}
	}
	
	@Test
	public void testRetiradaSinSaldo() {
		try {
			this.cuentaPepe.retirar(2000);
			fail("Esperaba SaldoInsuficienteException");
		} catch (ImporteInvalidoException e) {
			fail("Se ha producido ImporteInvalidoException");
		} catch (SaldoInsuficienteException e) {
		}
	}
	

	@Test
	public void testTransferencia() {
		try {
			this.cuentaPepe.transferir(this.cuentaAna.getId(), 500, "Alquiler");
			assertTrue(this.cuentaPepe.getSaldo() == 495);
			assertTrue(this.cuentaAna.getSaldo() == 5500);
		} catch (Exception e) {
			fail("Excepción inesperada: " + e.getMessage());
		}
	}
	
	@Test
	public void testCompraConTC() {
		try {
			cuentaPepe.retirar(200);
			assertTrue(cuentaPepe.getSaldo()==800);
			
			TarjetaCredito tc = cuentaPepe.emitirTarjetaCredito("12345X", 1000);
			tc.comprar(tc.getPin(), 300);
			assertTrue(tc.getCreditoDisponible()==700);
			tc.liquidar();
			assertTrue(tc.getCreditoDisponible()==1000);
			assertTrue(cuentaPepe.getSaldo()==500);
		} catch (Exception e) {
			fail("Excepción inesperada: " + e.getMessage());
		}
	}
	
	@Test
	public void testCompraPorInternetConTC() {
		try {
			this.cuentaPepe.retirar(200);
			assertTrue(this.cuentaPepe.getSaldo()==800);
			
			int token = this.tcPepe.comprarPorInternet(tcPepe.getPin(), 300);
			assertTrue(this.tcPepe.getCreditoDisponible()==2000);
			this.tcPepe.confirmarCompraPorInternet(token);
			assertTrue(this.tcPepe.getCreditoDisponible()==1700);
			this.tcPepe.liquidar();
			assertTrue(this.tcPepe.getCreditoDisponible()==2000);
			assertTrue(cuentaPepe.getSaldo()==500);
		} catch (Exception e) {
			fail("Excepción inesperada: " + e.getMessage());
		}
	}
	
	@Test
	public void testCompraPorInternetConTCPinInvalido() {
			try {
				this.tcPepe.comprarPorInternet(5679, 100);
			} catch (PinInvalidoException e) {
			} catch (Exception e) {
				fail("Esperaba PinInvalidoException");
			} 

	}
	
	@Test
	public void testBloqueoTarjeta() {
		
		try {
			this.tcPepe.comprarPorInternet(0000, 100);
		} catch (PinInvalidoException e) {
		} catch (Exception e) {
			fail("Esperaba PinInvalidoException");
		} 
		try {
			this.tcPepe.comprarPorInternet(1111, 100);
		} catch (PinInvalidoException e) {
		} catch (Exception e) {
			fail("Esperaba PinInvalidoException");
		} 
		try {
			this.tcPepe.comprarPorInternet(9999, 100);
		} catch (PinInvalidoException e) {
		} catch (Exception e) {
			fail("Esperaba PinInvalidoException");
		} 
		
		try {			
			this.tcPepe.comprarPorInternet(1234, 30);
			fail("Esperaba que saltase TarjetaBloqueadaException");
		}catch(TarjetaBloqueadaException e) {
		}catch(Exception e){
			fail("Esperaba TarjetaBloqueadaException");
		}
		
	}
	
	@Test
	public void testTransferencia_1() {
		try {
			this.cuentaPepe.transferir(-2l, 250.00, "Lo que sea");
			fail("Esperaba CuentaInvalidaException");
		}catch(CuentaInvalidaException e) {
		}catch(Exception e) {
			fail("Esperaba CuentaInvalidaException");
		}
	}
	
	@Test
	public void testTransferencia_2() {
		try {
			this.cuentaPepe.transferir(1l, 250.00, "Lo que sea");
			fail("Esperaba CuentaInvalidaException");
		}catch(CuentaInvalidaException e) {
		}catch(Exception e) {
			fail("Esperaba CuentaInvalidaException");
		}
	}
	
	
	@Test
	public void testTransferencia_3() {
		try {
			this.cuentaPepe.transferir(this.cuentaAna.getId(), -1.00, "Lo que sea"); //this.cuentaAna.getId() == 2l
			fail("Esperaba ImporteInvalidoException");
		}catch(ImporteInvalidoException e) {
		}catch(Exception e) {
			fail("Esperaba ImporteInvalidoException");
		}
	}
	
	@Test
	public void testTransferencia_4() {
		try {
			this.cuentaPepe.transferir(this.cuentaAna.getId(), 250.00, "Lo que sea"); //this.cuentaAna.getId() == 2l
            assertTrue(this.cuentaPepe.getSaldo()==(1000.00 - 250.00 - 250.00*0.01));
            assertTrue(this.cuentaAna.getSaldo()==(5000.00 + 250.00));
		}catch(Exception e) {
			fail("Excepción inesperada: " + e.getMessage());
		}
	}
	
	@Test
	public void testTransferencia_5() {
		try {
			this.cuentaPepe.transferir(this.cuentaAna.getId(), 1100.00, "Lo que sea"); //this.cuentaAna.getId() == 2l
			fail("Se esperaba SaldoInsuficienteException");
		}catch(SaldoInsuficienteException e){
		}catch(Exception e) {
			fail("Se esperaba SaldoInsuficienteException");
		}
	}
	
	@Test
	public void testTransferencia_6() {
		try {
			this.cuentaPepe.transferir(this.cuentaAna.getId(), 0.00, "Lo que sea"); //this.cuentaAna.getId() == 2l
			fail("Se esperaba ImporteInvalidoException");
		}catch(ImporteInvalidoException e){
		}catch(Exception e) {
			fail("Se esperaba ImporteInvalidoException");
		}
	}
	
	@Test
	public void testTransferencia_7() {
		try {
			this.cuentaPepe.transferir(3l, 250.00, "Lo que sea"); //this.cuentaAna.getId() == 2l
			fail("Se esperaba CuentaInvalidaException");
		}catch(CuentaInvalidaException e){
		}catch(Exception e) {
			fail("Se esperaba CuentaInvalidaException");
		}
	}
	
	@Test 
	public void testIngresar_1() {
		try {
			this.cuentaPepe.ingresar(-100.00);
			fail("Se esperaba ImporteInvalidoException");
		}catch(ImporteInvalidoException e){
		}catch(Exception e) {
			fail("Se esperaba ImporteInvalidoException");
		}
	}
	
	@Test 
	public void testIngresar_2() {
		try {
			this.cuentaPepe.ingresar(3000.00);
			assertTrue(this.cuentaPepe.getSaldo() == 4000.00);
		}catch(Exception e) {
			fail("Excepción inesperada: " + e.getMessage());
		}
	}
	
	@Test 
	public void testIngresar_3() {
		try {
			this.cuentaPepe.ingresar(0.00);
			fail("Se esperaba ImporteInvalidoException");
		}catch(ImporteInvalidoException e){
		}catch(Exception e) {
			fail("Se esperaba ImporteInvalidoException");
		}
	}

}
