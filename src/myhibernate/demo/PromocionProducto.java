package myhibernate.demo;

import myhibernate.ann.Column;
import myhibernate.ann.Entity;
import myhibernate.ann.Id;
import myhibernate.ann.JoinColumn;
import myhibernate.ann.Table;

@Entity
@Table(name="promocion_producto")
public class PromocionProducto {
    @Id
    @Column(name="id_promocion_vigencia")
    private int idPromocionVigencia;

    @JoinColumn(name="id_producto")
    private Producto producto;

    @Column(name="descuento")
    private double descuento;

    public int getIdPromocionVigencia() {
        return idPromocionVigencia;
    }

    public void setIdPromocionVigencia(int idPromocionVigencia) {
        this.idPromocionVigencia = idPromocionVigencia;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }
}
