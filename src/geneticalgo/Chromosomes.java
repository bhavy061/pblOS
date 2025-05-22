package geneticalgo;

import java.util.ArrayList;
import org.cloudbus.cloudsim.Vm;

public class Chromosomes {
	
	/** A list of Gene object */
	protected ArrayList<Gene> geneList;
	
	public Chromosomes(ArrayList<Gene> geneList){
		this.geneList = geneList;		
	}

	public ArrayList<Gene> getGeneList(){
		return this.geneList;
	}

	public void updateGene(int index, Vm vm){
		Gene gene = this.geneList.get(index);
		gene.setVmForGene(vm);
		this.geneList.set(index, gene);
	}
}