package myhibernate.demo;

import myhibernate.ann.Column;
import myhibernate.ann.Entity;
import myhibernate.ann.Id;
import myhibernate.ann.JoinColumn;
import myhibernate.ann.ManyToOne;
import myhibernate.ann.Table;

@Entity
@Table(name="producto")
public class Producto
{
   @Id
   @Column(name="id_producto")
   public int idProducto;
   
   @Column(name="descripcion_producto")
   public String descripcion;

   @ManyToOne
   @JoinColumn(name="id_proveedor")
   public Proveedor proveedor;

   @JoinColumn(name="id_categoria")
   public Categoria categoria;

   @Column(name="precio_unitario")
   public double precioUnitario;

   @Column(name="unidades_stock")
   public int unidadesStock;

   @Column(name="unidades_reposicion")
   public int unidadesReposicion;

   @Column(name="flg_discontinuo")
   public int flgDiscontinuo;

   public int getIdProducto() {
      return idProducto;
   }

   public void setIdProducto(int idProducto) {
      this.idProducto = idProducto;
   }

   public String getDescripcion() {
      return descripcion;
   }

   public void setDescripcion(String descripcion) {
      this.descripcion = descripcion;
   }

   public Proveedor getProveedor() {
      return proveedor;
   }

   public void setProveedor(Proveedor proveedor) {
      this.proveedor = proveedor;
   }

   public Categoria getCategoria() {
      return categoria;
   }

   public void setCategoria(Categoria categoria) {
      this.categoria = categoria;
   }

   public double getPrecioUnitario() {
      return precioUnitario;
   }

   public void setPrecioUnitario(double precioUnitario) {
      this.precioUnitario = precioUnitario;
   }

   public int getUnidadesStock() {
      return unidadesStock;
   }

   public void setUnidadesStock(int unidadesStock) {
      this.unidadesStock = unidadesStock;
   }

   public int getUnidadesReposicion() {
      return unidadesReposicion;
   }

   public void setUnidadesReposicion(int unidadesReposicion) {
      this.unidadesReposicion = unidadesReposicion;
   }

   public int getFlgDiscontinuo() {
      return flgDiscontinuo;
   }

   public void setFlgDiscontinuo(int flgDiscontinuo) {
      this.flgDiscontinuo = flgDiscontinuo;
   }
}
