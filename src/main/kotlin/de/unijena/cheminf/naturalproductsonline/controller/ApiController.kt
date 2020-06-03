package de.unijena.cheminf.naturalproductsonline.controller


import de.unijena.cheminf.naturalproductsonline.coconutmodel.mongocollections.UniqueNaturalProduct
import de.unijena.cheminf.naturalproductsonline.coconutmodel.mongocollections.UniqueNaturalProductRepository
import de.unijena.cheminf.naturalproductsonline.utils.AtomContainerToUniqueNaturalProductService
import org.openscience.cdk.exception.CDKException
import org.openscience.cdk.exception.InvalidSmilesException
import org.openscience.cdk.fingerprint.PubchemFingerprinter
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.isomorphism.Ullmann
import org.openscience.cdk.silent.SilentChemObjectBuilder
import org.openscience.cdk.smiles.SmiFlavor
import org.openscience.cdk.smiles.SmilesGenerator
import org.openscience.cdk.smiles.SmilesParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import org.springframework.data.mongodb.core.query.Query.query



@RestController
@RequestMapping("/api")
class ApiController(val uniqueNaturalProductRepository: UniqueNaturalProductRepository) {
    /*
    * custom api code goes here
    *
    * basic REST-API generated by Spring
    * see @RepositoryRestResource annotation in repository class(es)
    *
    */

    val smilesParser: SmilesParser = SmilesParser(SilentChemObjectBuilder.getInstance())
    val smilesGenerator: SmilesGenerator = SmilesGenerator(SmiFlavor.Unique)
    internal var pubchemFingerprinter = PubchemFingerprinter(SilentChemObjectBuilder.getInstance())

    @Autowired
    lateinit var atomContainerToUniqueNaturalProductService: AtomContainerToUniqueNaturalProductService

    @RequestMapping("/search/exact/structure")
    fun structureSearchBySmiles(@RequestParam("smiles") smiles: String): Map<String, Any> {
        return this.doStructureSearchBySmiles(URLDecoder.decode(smiles.trim(), "UTF-8"))
    }

    @RequestMapping("/search/substructure")
    fun substructureSearch(@RequestParam("smiles") smiles: String): Map<String, Any> {
        return this.doSubstructureSearch(URLDecoder.decode(smiles.trim(), "UTF-8"))
    }

    @RequestMapping("/search/simple")
    fun simpleSearch(@RequestParam("query") queryString: String): Map<String, Any> {
        /* switch between simple and simple heuristic search
        * the latter tries to guess the input type that could become harder with more search options
        */
        println(queryString)
        return this.doSimpleSearchWithHeuristic(URLDecoder.decode(queryString.trim(), "UTF-8"))
        // return this.doSimpleSearch(URLDecoder.decode(queryString.trim(), "UTF-8"))
    }

    fun doStructureSearchBySmiles(smiles: String): Map<String, Any> {
        try {
            val parsedSmiles: IAtomContainer = this.smilesParser.parseSmiles(smiles)
            val canonicalSmiles: String = this.smilesGenerator.create(parsedSmiles)

            val results = this.uniqueNaturalProductRepository.findByClean_smiles(canonicalSmiles)

            return mapOf(
                    "originalQuery" to canonicalSmiles,
                    "count" to results.size,
                    "naturalProducts" to results
            )

        } catch (e: InvalidSmilesException) {
            error("An InvalidSmilesException occured: ${e.message}")
        } catch (e: CDKException) {
            error("A CDKException occured: ${e.message}")
        }
    }

    fun doSimpleSearch(query: String): Map<String, Any> {
        val naturalProducts = mutableSetOf<UniqueNaturalProduct>()

        naturalProducts += this.uniqueNaturalProductRepository.findBySmiles(query)
        naturalProducts += this.uniqueNaturalProductRepository.findByClean_smiles(query)
        naturalProducts += this.uniqueNaturalProductRepository.findByInchi(query)
        naturalProducts += this.uniqueNaturalProductRepository.findByInchikey(query)
        naturalProducts += this.uniqueNaturalProductRepository.findByMolecular_formula(query)
        naturalProducts += this.uniqueNaturalProductRepository.findByCoconut_id(query)

        return mapOf(
                "originalQuery" to query,
                "naturalProducts" to naturalProducts
        )
    }

    fun doSimpleSearchWithHeuristic(query: String): Map<String, Any> {
        // determine type of input on very basic principles without validation

        var queryType = "unknown"


        var inchiPattern = Regex("^InChI=.*$")
        val inchikeyPattern = Regex("^[A-Z]{14}-[A-Z]{10}-[A-Z]$")
        val molecularFormulaPattern = Regex("C[0-9]+?H[0-9].+")
        //var smilesPattern = Regex("^([^Jj][A-Za-z0-9@+\\-\\[\\]\\(\\)\\\\\\/%=#\$]+)\$")
        val coconutPattern = Regex("^CNP[0-9]+?$")

        var naturalProducts : List<UniqueNaturalProduct>
        val determinedInputType : String


        /*if(smilesPattern.containsMatchIn(query)){
             naturalProducts =  this.uniqueNaturalProductRepository.findBySmiles(query)
             determinedInputType = "SMILES"
        }
        else */
        if(coconutPattern.containsMatchIn(query)){
             naturalProducts =  this.uniqueNaturalProductRepository.findByCoconut_id(query)
             determinedInputType = "COCONUT ID"
        }
        else if(inchiPattern.containsMatchIn(query)){
             naturalProducts =  this.uniqueNaturalProductRepository.findByInchi(query)
             determinedInputType = "InChi"
        }
        else if(inchikeyPattern.containsMatchIn(query)){
             naturalProducts =  this.uniqueNaturalProductRepository.findByInchikey(query)
             determinedInputType = "InChi Key"
        }
        else if(molecularFormulaPattern.containsMatchIn(query)){
            naturalProducts = this.uniqueNaturalProductRepository.findByMolecular_formula(query)
             determinedInputType = "molecular formula"
        }
        else{
            //try to march by name
             naturalProducts = this.uniqueNaturalProductRepository.findByName(query)

            if(naturalProducts == null || naturalProducts.isEmpty()){
                naturalProducts = this.uniqueNaturalProductRepository.fuzzyNameSearch(query)
            }
             determinedInputType = "name"
        }




        return mapOf(
                "originalQuery" to query,
                "determinedInputType" to determinedInputType,
                "naturalProducts" to naturalProducts
        )
    }






    fun doSubstructureSearch(smiles: String): Map<String, Any> {
        println("Entering substructure search")

        println(smiles)

        try {
            val queryAC: IAtomContainer = this.smilesParser.parseSmiles(smiles)

            println(pubchemFingerprinter.getBitFingerprint(queryAC).asBitSet().toByteArray())

            // run $allBitsSet in mongo
            val matchedList = this.uniqueNaturalProductRepository.findAllPubchemBitsSet(pubchemFingerprinter.getBitFingerprint(queryAC).asBitSet().toByteArray())

            println("found molecules with bits set")

            // return a list of UNP:
            // for each UNP - convert to IAC and run the Ullmann
            val pattern = Ullmann.findSubstructure(queryAC)
            val hits = mutableListOf<UniqueNaturalProduct>()
            var hitsCount: Int = 0

            //Stop at first 250? Stop at random 250?

            for( unp in  matchedList){
                var targetAC : IAtomContainer = this.atomContainerToUniqueNaturalProductService.createAtomContainer(unp)

                val match = pattern.match(targetAC);

                // do not save all hits since this would have insane memory requirements for simple and often reoccurring substructures
                if (match.isNotEmpty()) {
                    hitsCount++
                    hits.add(unp)

                    println(unp.coconut_id)

                }
            }

            println("ready to return results!")

            return mapOf(
                    "originalQuery" to smiles,
                    "count" to hitsCount,
                    "naturalProducts" to hits
            )

        } catch (e: InvalidSmilesException) {
            error("An InvalidSmilesException occured: ${e.message}")
        } catch (e: CDKException) {
            error("A CDKException occured: ${e.message}")
        }
    }
}