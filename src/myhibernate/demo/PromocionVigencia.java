package myhibernate.demo;

import myhibernate.ann.Column;
import myhibernate.ann.Entity;
import myhibernate.ann.Id;
import myhibernate.ann.JoinColumn;
import myhibernate.ann.Table;

import java.util.Date;

@Entity
@Table(name="promocion_vigencia")
public class PromocionVigencia {

    @Id
    @Column(name="id_promocion_vigencia")
    public int idPromocionVigencia;

    @JoinColumn(name="promocion")
    public Promocion promocion;

    @Column(name="fecha_inicio")
    public Date fechaInicio;

    @Column(name="fecha_fin")
    public Date fechaFin;

    public int getIdPromocionVigencia() {
        return idPromocionVigencia;
    }

    public void setIdPromocionVigencia(int idPromocionVigencia) {
        this.idPromocionVigencia = idPromocionVigencia;
    }

    public Promocion getPromocion() {
        return promocion;
    }

    public void setPromocion(Promocion promocion) {
        this.promocion = promocion;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }
}
